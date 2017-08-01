(ns patreon.core
  (:require [clojure.string :as string]
            [cljs.spec.alpha :as s]))

(def node-fs (js/require "fs"))
(defn read-file [path] (.readFileSync node-fs path "utf-8"))
(defn write-file [path text] (.writeFileSync node-fs path text))
(defn parse-float [s] (js/parseFloat s 10))


(def file-paths ["./reports/November Patreon Report.csv"
                 "./reports/December Patreon Report.csv"
                 "./reports/January Patreon Report.csv"
                 "./reports/February Patreon Report.csv"
                 "./reports/March Patreon Report.csv"
                 "./reports/April Patreon Report.csv"
                 "./reports/May Patreon Report.csv"
                 "./reports/PatronReportJune.csv"])


(s/def ::row (s/cat :first string?
                    :last string?
                    :email (s/and string?
                                  #(re-matches #"^.+@.+\..+$" %))
                    :pledge (s/and string?
                                   #(not (js/isNaN (parse-float %))))))


(defn row->map [row]
  (let [row-vec (string/split row #",") ;; split line by commas
        parsed-row (s/conform ::row row-vec)] ;; conform row string to map using the spec
    (if (= parsed-row ::s/invalid)
      (throw (js/Error. "Invalid row data!"))
      (update parsed-row :pledge parse-float)))) ;; convert :pledge to float


(def data
  "A list of lists of pledges with the shape {:first :last :email :pledge}"
  (->> file-paths
       (map (fn [file-path]
              (let [file (read-file file-path)
                    rows (drop 2 (string/split-lines file))] ;; split by line breaks and remove header rows
                (map row->map rows))))))


(def users
  "A list of all users with the shape {:first :last :email}"
  (->> (apply concat data) ;; get a flat list of all rows
    (reduce
      (fn [acc {:keys [email] :as row}]
        (assoc acc email (select-keys row [:first :last :email]))) ;; key user data by email to dedupe users
      {})
    vals)) ;; then only return the vals


(defn email->pledges
  "Takes an email and returns a list of pledge amounts (one for each month)"
  [email]
  (map (fn [file]
         (let [row (some #(when (= email (:email %)) %) file)] ;; find row by email
           (get row :pledge 0))) ;; get pledge amount or use zero as default
       data))


(def users+totals
  "A list of all users with the shape {:first :last :email :pledges :total}"
  (map (fn [{:keys [email] :as user}]
         (let [pledges (email->pledges email)]
           (assoc user :pledges pledges
                       :total (apply + pledges))))
       users))


(defn users+totals->csv [totals]
  (str "FirstName,LastName,Email,Pledge,Nov,Dec,Jan,Feb,Mar,Apr,May,June\n,,,,,,,,,,,\n"
       (string/join "\n"
         (map (fn [{:keys [first last email total pledges]}]
                (string/join "," [first last email total (string/join "," (map #(if (zero? %) "" %) pledges))]))
              totals))))


(->> users+totals
     (sort-by :total >)
     (users+totals->csv)
     (write-file "./totals.cljs.csv"))


(println "Success!")
