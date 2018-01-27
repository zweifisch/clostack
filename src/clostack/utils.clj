(ns clostack.utils
  (:require [clj-http.client :as client]
            [clojure.string :as s]
            ;; [cheshire.core :as json]
            [clojure.data.json :as json]))

(declare ^:dynamic *proxy-host*)
(declare ^:dynamic *proxy-port*)

(defn dash-key [m]
  (into {} (for [[k v] m]
             [(s/replace (subs (str k) 1) #"-" "_") v])))

(defn http-proxy-from-env []
  (when-let [http-proxy (System/getenv "http_proxy")]
    (let [url (java.net.URL. http-proxy)]
      {:proxy-host (.getHost url)
       :proxy-port (.getPort url)})))

(defn http-proxy []
  (if (bound? #'*proxy-host* #'*proxy-port*)
    {:proxy-host *proxy-host*
     :proxy-port *proxy-port*}
    (http-proxy-from-env)))

(defn list-map
  ([l key] (zipmap (map key l) l))
  ([l key val] (zipmap (map key l) (map val l))))

(defn request [token method url & {:keys [body query as accept content-type] :or {as :json accept :json content-type :json}}]
  (with-redefs [clj-http.client/json-enabled? true]
    (binding [clj-http.client/json-encode (fn [input _] (json/write-str input))
              clj-http.client/json-decode (fn [input _] (json/read-str input :key-fn keyword))]
      (client/request (merge {:method method
                              :url url
                              :headers {:X-Auth-Token (:token token)}
                              :query-params (dash-key query)
                              :content-type content-type
                              :accept accept
                              :as as}
                             (if (or (map? body) (vector? body))
                               {:form-params body}
                               {:body body})
                             (http-proxy))))))

(defn endpoint-get [token service & [interface region]]
  (let [region (or region (:region token))
        interface (or interface (:interface token) "public")
        url (->> token
                 :catalog (filter #(= service (:type %))) first
                 :endpoints (filter #(and (or (not region) (= region (:region %)))
                                          (= interface (:interface %))))
                 first
                 :url)]
    (if (= service "identity") (s/replace-first url "/v2.0" "/v3") url)))

(defn join [tokens tokens2]
  (interleave tokens (concat tokens2 [""])))

(defmacro defres [name service url singular plural & {:keys [custom-actions only put-for-update] :or {only '(list list-pager get delete update create) custom-actions [] put-for-update false}}]
  (let [list (symbol (str name "-list"))
        list-pager (symbol (str name "-list-pager"))
        get (symbol (str name "-get"))
        delete (symbol (str name "-delete"))
        update (symbol (str name "-update"))
        create (symbol (str name "-create"))
        pargs (map (comp symbol second) (re-seq #":([^:/]+)" url))
        actions (set only)
        segments (join (s/split url #":[^:/]+") pargs)]
    `(do
       ~@(->> `(~'list
                (def ~list
                  (fn [token# ~@pargs & [options#]]
                    (-> (request token# :get (str (endpoint-get token# ~service) ~@segments) :query options#) :body ~plural)))
                ~'list-pager
                (def ~list-pager
                  (fn [token# ~@pargs & [options#]]
                    (-> (request token# :get (str (endpoint-get token# ~service) ~@segments) :query options#) :body)))
                ~'get
                (def ~get
                  (fn [token# ~@pargs id#]
                    (-> (request token# :get (str (endpoint-get token# ~service) ~@segments "/" id#)) :body ~@(if singular `(~singular) '()))))
                ~'delete
                (def ~delete
                  (fn [token# ~@pargs id#]
                    (-> (request token# :delete (str (endpoint-get token# ~service) ~@segments "/" id#)))))
                ~'update
                (def ~update
                  (fn [token# ~@pargs id# ~'body]
                    (-> (request token# ~(if put-for-update :put :patch) (str (endpoint-get token# ~service) ~@segments "/" id#) :body ~(if (nil? singular) 'body {singular 'body}))
                        :body ~@(if singular `(~singular) '()))))
                ~'create
                (def ~create
                  (fn [token# ~@pargs ~'body]
                    (-> (request token# :post (str (endpoint-get token# ~service) ~@segments) :body ~(if (nil? singular) 'body {singular 'body})) :body ~@(if singular `(~singular) '())))))
              (partition 2)
              (filter (comp actions first))
              (map last))
       ~@(for [[f method segment] custom-actions
               :let [name (symbol (str name "-" f))]]
           (case method
             (:delete :get)
             `(def ~name
                (fn
                  ([token# ~@pargs id# sub-id# & [options#]]
                   (-> (request token# ~method (str (endpoint-get token# ~service) ~@segments "/" id# ~segment "/" sub-id#) :query options#)))
                  ([token# ~@pargs id#]
                   (-> (request token# ~method (str (endpoint-get token# ~service) ~@segments "/" id# ~segment))))
                  ([token# ~@pargs]
                   (-> (request token# ~method (str (endpoint-get token# ~service) ~@segments ~segment))))))
             `(def ~name
                (fn
                  ([token# ~@pargs id# body#]
                   (-> (request token# ~method (str (endpoint-get token# ~service) ~@segments "/" id# ~segment) :body body#)))
                  ([token# ~@pargs id# sub-id# body# & [options#]]
                   (-> (request token# ~method (str (endpoint-get token# ~service) ~@segments "/" id# ~segment "/" sub-id#) :query options#))))))))))

#_
(macroexpand '(defres server "compute" "/servers/id/attachments" nil :servers :custom-actions [[action :post "/action"]]))

#_
(macroexpand '(defres server "compute" "/servers/id/attachments" :server :servers))

#_
(defres server "compute" "/servers/id/attachments" :server :servers)

#_
(macroexpand '(defres image "image" "/v2/images" nil :images :put-for-update true))

#_
(macroexpand '(defres image "image" "/v2/images" nil :images :only [list]))

#_
(defres image "image" "/v2/images" nil :images :only [list])
