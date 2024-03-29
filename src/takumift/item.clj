(ns takumift.item
  (:require [takumift.material :as m]))

(def swords #{m/wood-sword m/stone-sword m/iron-sword
              m/gold-sword m/diamond-sword})

(def pickaxe-durabilities
  {m/wood-pickaxe 60 m/stone-pickaxe 132 m/iron-pickaxe 251 m/gold-pickaxe 33
   m/diamond-pickaxe 1562})

(def pickaxes
  (set (keys pickaxe-durabilities)))

(def hoe-durabilities
  {m/wood-hoe 60 m/stone-hoe 132 m/iron-hoe 251 m/gold-hoe 33
   m/diamond-hoe 1562})

(def records
  [m/gold-record m/green-record m/record-10
   m/record-11 m/record-3 m/record-4 m/record-5
   m/record-6 m/record-7 m/record-8 m/record-9])

(def unobtainable
  #{m/mob-spawner m/web m/monster-eggs})

(defn modify-durability [item f]
  (.setDurability item (f (.getDurability item))))

