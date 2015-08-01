(ns clostack.utils
  (:require [clj-http.client :as client]
            [clojure.string :as s]))

(declare ^:dynamic *proxy-host*)
(declare ^:dynamic *proxy-port*)

(defn dash-key [m]
  (into {} (for [[k v] m]
             [(s/replace (subs (str k) 1) #"-" "_") v])))

(defn list-map
  ([l key] (zipmap (map key l) l))
  ([l key val] (zipmap (map key l) (map val l))))

(defn request [token method url & {:keys [body query]}]
  (client/request (merge {:method method
                          :url url
                          :headers {:X-Auth-Token (:token token)}
                          :form-params body
                          :query-params (dash-key query)
                          :content-type :json
                          :accept :json
                          :as :json}
                         (when (bound? #'*proxy-host* #'*proxy-port*)
                           {:proxy-host *proxy-host*
                            :proxy-port *proxy-port*})
                         )))

(defn endpoint-get [token service & [interface region]]
  (->> token
       :catalog (filter #(= service (:type %))) first
       :endpoints (filter #(and (or (not region) (= region (:region %)))
                                (= (or interface "public") (:interface %))))
       first :url))

(defmacro defres [name service url singular plural & more]
  (let [list (symbol (str name "-list"))
        list-pager (symbol (str name "-list-pager"))
        get (symbol (str name "-get"))
        delete (symbol (str name "-delete"))
        update (symbol (str name "-update"))
        create (symbol (str name "-create"))]
    `(do
       (def ~list
         (fn [token# & [options#]]
           (-> (request token# :get (str (endpoint-get token# ~service) ~url) :query options#) :body ~plural)))
       (def ~list-pager
         (fn [token# & [options#]]
           (-> (request token# :get (str (endpoint-get token# ~service) ~url) :query options#) :body)))
       (def ~get
         (fn [token# id#]
           (-> (request token# :get (str (endpoint-get token# ~service) ~url "/" id#)) :body ~singular)))
       (def ~delete
         (fn [token# id#]
           (-> (request token# :delete (str (endpoint-get token# ~service) ~url "/" id#)))))
       (def ~update
         (fn [token# id# body#]
           (-> (request token# :patch (str (endpoint-get token# ~service) ~url "/" id#) :body {~singular body#}))))
       (def ~create
         (fn [token# body#]
           (-> (request token# :post (str (endpoint-get token# ~service) ~url) :body {~singular body#}))))
       ~@(for [[f method segment] more
               :let [name (symbol (str name "-" f))]]
           (case method
             (:delete :get)
             `(def ~name
                (fn [token# id# sub-id# & [options#]]
                  (-> (request token# ~method (str (endpoint-get token# ~service) ~url "/" id# ~segment "/" sub-id#) :query options#))))
             `(def ~name
                (fn [token# id# body#]
                  (-> (request token# ~method (str (endpoint-get token# ~service) ~url "/" id# ~segment) :body body#)))))))))
