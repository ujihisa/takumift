(ns takumift.takumift
  (:require [swank.swank])
  (:require [clojure.string :as s])
  (:require [clojure.set])
  (:require [takumift.material :as m])
  (:import [org.bukkit Bukkit Material])
  (:import [org.bukkit.entity Animals Arrow Blaze Boat CaveSpider Chicken
            ComplexEntityPart ComplexLivingEntity Cow Creature Creeper Egg
            EnderCrystal EnderDragon EnderDragonPart Enderman EnderPearl
            EnderSignal ExperienceOrb Explosive FallingSand Fireball Fish
            Flying Ghast Giant HumanEntity Item LightningStrike LivingEntity
            MagmaCube Minecart Monster MushroomCow NPC Painting Pig PigZombie
            Player PoweredMinecart Projectile Sheep Silverfish Skeleton Slime
            SmallFireball Snowball Snowman Spider Squid StorageMinecart
            ThrownPotion TNTPrimed Vehicle Villager WaterMob Weather Wolf
            Zombie])
  (:import [org.bukkit.event.entity CreatureSpawnEvent CreeperPowerEvent
            EntityChangeBlockEvent
            EntityCombustByBlockEvent EntityCombustByEntityEvent
            EntityCombustEvent EntityCreatePortalEvent EntityDamageByBlockEvent
            EntityDamageByEntityEvent
            EntityDamageEvent EntityDeathEvent EntityEvent EntityExplodeEvent
            EntityDamageEvent$DamageCause
            EntityInteractEvent EntityPortalEnterEvent
            EntityRegainHealthEvent EntityShootBowEvent EntityTameEvent
            EntityTargetEvent ExplosionPrimeEvent
            FoodLevelChangeEvent ItemDespawnEvent ItemSpawnEvent PigZapEvent
            PlayerDeathEvent PotionSplashEvent ProjectileHitEvent
            SheepDyeWoolEvent SheepRegrowWoolEvent SlimeSplitEvent])
  (:import [org.bukkit.potion PotionType]))

(defonce plugin* nil)
(defmacro later [& exps]
  `(.scheduleSyncDelayedTask
     (Bukkit/getScheduler)
     plugin*
     (fn [] ~@exps)
     0))

(defn init-plugin [plugin]
  (when-not plugin*
    (def plugin* plugin)))

(def world (Bukkit/getWorld "world"))

(defn broadcast [& strs]
  (.broadcastMessage (Bukkit/getServer) (apply str strs)))

(defn location-in-lisp [location]
  (list
    'org.bukkit.Location.
    (.getName (.getWorld location))
    (.getX location)
    (.getY location)
    (.getZ location)
    (.getPitch location)
    (.getYaw location)))

(defn swap-entity [target klass]
  (let [location (.getLocation target)
        world (.getWorld target)]
    (.remove target)
    (.spawn world location klass)))

(defn consume-item [player]
  (let [itemstack (.getItemInHand player)
        amount (.getAmount itemstack)]
    (if (= 1 amount)
      (.remove (.getInventory player) itemstack)
      (.setAmount itemstack (dec amount)))))

(defn get-player [name]
  (first (filter #(= name (.getDisplayName %)) (Bukkit/getOnlinePlayers))))

(defn ujm [] (get-player "ujm"))

(defn jumping? [moveevt]
  (< (.getY (.getFrom moveevt)) (.getY (.getTo moveevt))))

(defn consume-itemstack [inventory mtype]
  (let [idx (.first inventory mtype)
        itemstack (.getItem inventory idx)
        amount (.getAmount itemstack)]
    (if (= 1 amount)
      (.remove inventory itemstack)
      (.setAmount itemstack (dec amount)))))

(defn location-bound? [loc min max]
  (.isInAABB (.toVector loc) (.toVector min) (.toVector max)))

(defn add-velocity [entity x y z]
  (.setVelocity entity (.add (.getVelocity entity) (org.bukkit.util.Vector. (double x) (double y) (double z)))))

(defn entities-nearby-from [location radius]
  "location -> set of entities"
  (let [players
        (filter #(> radius (.distance (.getLocation %) location)) (Bukkit/getOnlinePlayers))]
    (apply clojure.set/union (map #(set (cons % (.getNearbyEntities % radius radius radius))) players))))

(defn removable-block? [block]
  (and
    (nil? (#{Material/AIR Material/CHEST Material/FURNACE
             Material/BURNING_FURNACE Material/BEDROCK} (.getType block)))
    (not (.isLiquid block))))

(defn entity2name [entity]
  (cond (instance? Blaze entity) "Blaze"
        (instance? CaveSpider entity) "CaveSpider"
        (instance? Chicken entity) "Chicken"
        ;(instance? ComplexLivingEntity entity) "ComplexLivingEntity"
        (instance? Cow entity) "Cow"
        ;(instance? Creature entity) "Creature"
        (instance? Creeper entity) "Creeper"
        (instance? EnderDragon entity) "EnderDragon"
        (instance? Enderman entity) "Enderman"
        ;(instance? Flying entity) "Flying"
        (instance? Ghast entity) "Ghast"
        (instance? Giant entity) "Giant"
        ;(instance? HumanEntity entity) "HumanEntity"
        (instance? MagmaCube entity) "MagmaCube"
        ;(instance? Monster entity) "Monster"
        (instance? MushroomCow entity) "MushroomCow"
        ;(instance? NPC entity) "NPC"
        (instance? Pig entity) "Pig"
        (instance? PigZombie entity) "PigZombie"
        (instance? Player entity) (.getDisplayName entity)
        (instance? Sheep entity) "Sheep"
        (instance? Silverfish entity) "Silverfish"
        (instance? Skeleton entity) "Skeleton"
        (instance? Slime entity) "Slime"
        (instance? Snowman entity) "Snowman"
        (instance? Spider entity) "Spider"
        (instance? Squid entity) "Squid"
        (instance? Villager entity) "Villager"
        ;(instance? WaterMob entity) "WaterMob"
        (instance? Wolf entity) "Wolf"
        (instance? Zombie entity) "Zombie"
        (instance? TNTPrimed entity) "TNT"
        (instance? Fireball entity) "Fireball"
        :else (last (clojure.string/split (str (class entity)) #"\."))))

(defn move-entity [entity x y z]
  (let [loc (.getLocation entity)]
    (.add loc x y z)
    (.teleport entity loc)))

(defn teleport-without-angle [entity location]
  (.setYaw location (.getYaw (.getLocation entity)))
  (.setPitch location (.getPitch (.getLocation entity)))
  (.teleport entity location))

(defn freeze [target sec]
  (when-not (.isDead target)
    (let [loc (.getLocation (.getBlock (.getLocation target)))]
      (doseq [y [0 1]
              [x z] [[-1 0] [1 0] [0 -1] [0 1]]
              :let [block (.getBlock (.add (.clone loc) x y z))]
              :when (#{m/air m/snow} (.getType block))]
        (.setType block m/glass))
      (doseq [y [-1 2]
              :let [block (.getBlock (.add (.clone loc) 0 y 0))]
              :when (#{m/air m/snow} (.getType block))]
        (.setType block m/glass))
      (future
        (Thread/sleep 1000)
        (when-not (.isDead target)
          (later (.teleport target (.add (.clone loc) 0.5 0.0 0.5)))))
      (future
        (Thread/sleep (* sec 1000))
        (doseq [y [0 1]
                [x z] [[-1 0] [1 0] [0 -1] [0 1]]
                :let [block (.getBlock (.add (.clone loc) x y z))]
                :when (= (.getType block) m/glass)]
          (later (.setType block m/air)))
        (doseq [y [-1 2]
                :let [block (.getBlock (.add (.clone loc) 0 y 0))]
                :when (= (.getType block) m/glass)]
          (later (.setType block m/air)))))))
(defn freeze-for-20-sec [target]
  (freeze target 20))

(def potion-types [PotionType/FIRE_RESISTANCE PotionType/INSTANT_DAMAGE
                   PotionType/INSTANT_HEAL PotionType/POISON PotionType/REGEN
                   PotionType/SLOWNESS PotionType/SPEED PotionType/STRENGTH
                   PotionType/WATER PotionType/WEAKNESS])
