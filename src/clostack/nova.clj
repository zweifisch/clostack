(ns clostack.nova
  (:require [clostack.utils :as utils]
            [clj-http.client :as client]
            [clojure.string :as string]))

(defmacro defres [name url singular plural & more]
  `(utils/defres ~name "compute" ~url ~singular ~plural ~@more :put-for-update true))

(defn gen-url [token & segments]
  (apply str (cons (utils/endpoint-get token "compute") segments)))

(defres server-detail "/servers/detail" :server :servers)

(defres server "/servers" :server :servers :custom-actions
  [[action :post "/action"]])

(defres server-interface-attachment "/servers/:server-id/os-interface" :interfaceAttachment :interfaceAttachments)

(defres server-volume-attachment "/servers/:server-id/os-volume_attachments" :volumeAttachment :volumeAttachments)

(defn server-get-console [token id type]
  (-> (server-action token id {:os-getVNCConsole {:type type}}) :body :console))

(defn server-get-console-output [token id length]
  (-> (server-action token id {:os-getConsoleOutput {:length length}}) :body :output))

(defres server-tag "/servers/:server-id/tags" :tag :tags)

(defres flavor "/flavors" :flavor :flavors)

(defres flavor-detail "/flavors/detail" :flavor :flavors)

(defres keypair "/os-keypairs" :keypair :keypairs)

(defres aggregate "/os-aggregates" :aggregate :aggregates :custom-actions
  [[action :post "/action"]])

(defres host "/os-hosts" :host :hosts)

(defres hypervisor "/os-hypervisors" :hypervisor :hypervisors :custom-actions
  [[uptime :get "/uptime"]])

(defres hypervisor-statistic "/os-hypervisors/statistics" identity :hypervisor_statistics)

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

(defres version "/" :version :version :only [get list])

(defres availability-zone "/os-availability-zone" nil :availabilityZoneInfo :only [list])

(defres availability-zone-detail "/os-availability-zone/detail" nil :availabilityZoneInfo :only [list])

(defres aggregate "/os-aggregates" :aggregate :aggregates :custom-actions
  [[action :post "/action"]
   [update :put ""]])

(defn aggregate-add-host [token id host]
  (aggregate-action token id {:add_host {:host host}}))

(defn aggregate-remove-host [token id host]
  (aggregate-action token id {:remove_host {:host host}}))

(defres simple-tenant-usage "/os-simple-tenant-usage" :tenant_usage :tenant_usages :only [list get])
