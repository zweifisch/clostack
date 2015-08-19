(ns clostack.nova
  (:require [clostack.utils :as utils]
            [clj-http.client :as client]
            [clojure.string :as string]))

(defmacro defres [name url singular plural & more]
  `(utils/defres ~name "compute" ~url ~singular ~plural ~@more))

(defn gen-url [token & segments]
  (apply str (cons (utils/endpoint-get token "compute") segments)))

(defres server-detail "/servers/detail" :server :servers)

(defres server "/servers" :server :servers
  [interface-detach :delete "/os-interface"]
  [interface-create :post "/os-interface"]
  [volume-attach :post "/os-volume_attachments"]
  [volume-detach :delete "/os-volume_attachments"]
  [action :post "/action"])

(defn server-get-console [token id type]
  (-> (server-action token id {:os-getVNCConsole {:type type}}) :body :console))

(defn server-get-console-output [token id length]
  (-> (server-action token id {:os-getConsoleOutput {:length length}}) :body :output))

(defres flavor "/flavors/detail" :flavor :flavors)

(defres keypair "/os-keypairs" :keypair :keypairs)

(defres aggregate "/os-aggregates" :aggregate :aggregates
  [action :post "/action"])

(defres host "/os-hosts" :host :hosts)

(defres hypervisor "/os-hypervisors" :hypervisor :hypervisors
  [migratenode-list :get "/migratenodes"])

(defres service "/os-services" :service :services)

(defn aggregate-set-metadata [token id metadata]
  (aggregate-action {:set_metadata {:metadata metadata}}))

(defres limit "/limits" :limit :limits)

(defn get-link [resp key rel]
  (->> resp key (filter #(= rel (:rel %))) first :href))

(defn fetch-next [token resp key]
  (some-> (get-link resp key "next")
          (string/replace-first #"\?" "/detail?")
          (client/get {:headers {:X-Auth-Token (:token token)}
                       :as :json})
          :body))

(defn server-list-all [token & [options]]
  (->> (server-list-pager token options)
       (iterate #(fetch-next token % :servers_links))
       (take-while (complement nil?))
       (map :servers)
       flatten))

(defn flavor-map [token]
  (let [flavors (-> (flavor-list token))]
    (zipmap (map :id flavors) flavors)))
