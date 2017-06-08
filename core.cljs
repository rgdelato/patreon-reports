(ns patreon.core
  (:require [clojure.string :as string]))
            ; [clojure.pprint :refer [pprint]]))

(def node-fs (js/require "fs"))

(defn read-file [path]
  (.readFileSync node-fs path "utf-8"))

(defn write-file [path text]
  (.writeFileSync node-fs path text))


(def file-paths ["./reports/November Patreon Report.csv"
                 "./reports/December Patreon Report.csv"
                 "./reports/January Patreon Report.csv"
                 "./reports/February Patreon Report.csv"
                 "./reports/March Patreon Report.csv"
                 "./reports/April Patreon Report.csv"
                 "./reports/May Patreon Report.csv"
                 "./reports/PatronReportJune.csv"])


(defn split-row
  "Data check: Throws error if the row doesn't have the correct number of fields"
  [row-str]
  (let [row-vec (string/split row-str #",")]
    (if (not= (count row-vec) 4)
      (throw (js/Error. "Invalid row data!"))
      row-vec)))


(defn parse-float
  "Data check: Throws error if string isn't a number"
  [s]
  (let [number (js/parseFloat s 10)]
    (if (js/isNaN number)
      (throw (js/Error. "Invalid pledge amount!"))
      number)))


(def data
  "A list of lists of pledges with the shape {:first :last :email :pledge}"
  (->> file-paths
       (map read-file)
       (map (fn [file]
              (let [rows (drop 2 (string/split-lines file))] ;; split by line breaks and remove header rows
                (->> rows
                     (map #(zipmap [:first :last :email :pledge] (split-row %))) ;; convert row string to map
                     (map #(update % :pledge parse-float)))))))) ;; convert :pledge to number


(def users
  "A list of all users with the shape {:first :last :email}"
  (->> (apply concat data) ;; get a flat list of all rows
    (reduce
      (fn [acc {:keys [email] :as row}]
        (assoc acc email (select-keys row [:first :last :email]))) ;; key user data by email to dedupe users
      {})
    vals)) ;; then only return the vals


(defn email->pledges
  "Takes an email and returns a vector of pledge amounts (one for each month)"
  [email]
  (map (fn [file]
         (let [row (some #(when (= email (:email %)) %) file)] ;; find row by email
           (get row :pledge 0))) ;; get pledge amount or use zero as default
       data))


(def users+totals
  "A list of all users with the shape {:first :last :email :pledges :total}"
  (map (fn [{:keys [email] :as user}]
         (let [pledges (email->pledges email)]
           (-> user
               (assoc :pledges pledges)
               (assoc :total (apply + pledges)))))
       users))


(defn users->csv [totals]
  (str "FirstName,LastName,Email,Pledge,Nov,Dec,Jan,Feb,Mar,Apr,May,June\n,,,,,,,,,,,\n"
       (string/join "\n"
        (map (fn [{:keys [first last email total pledges]}]
               (string/join "," [first last email total (string/join "," (map #(if (zero? %) "" %) pledges))]))
             totals))))


(->> users+totals
     (sort-by :total >)
     (users->csv)
     (write-file "./totals.cljs.csv"))


(println "Success!")
