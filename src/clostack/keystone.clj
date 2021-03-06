(ns clostack.keystone
  (:require [clj-http.client :as client]
            [clojure.set :refer [rename-keys]]
            [clostack.utils :as utils]
            ;; [cheshire.core :as json]
            [clojure.data.json :as json]))

(defmulti token-create (fn [_ & [params]] (map? params)))

(defmethod token-create false [url &{:keys [domain-name domain-id name password scope] :or {domain-name "default"}}]
  (token-create url {:domain-name domain-name
                     :domain-id domain-id
                     :name name
                     :password password
                     :scope scope}))

(defmethod token-create true [url {:keys [domain-name domain-id name password scope] :or {domain-name "default"}}]
  (with-redefs [clj-http.client/json-enabled? true]
    (binding [clj-http.client/json-encode (fn [input _] (json/write-str input))
              clj-http.client/json-decode (fn [input _] (json/read-str input :key-fn keyword))]
      (let [body
            {:auth (merge {:identity
                           {:methods ["password"]
                            :password {:user {:domain {:name domain-name :id domain-id}
                                              :name name
                                              :password password}}}}
                          (when scope {:scope scope}))}
            resp (client/post url (merge {:form-params body
                                          :content-type :json
                                          :accept :json
                                          :as :json}
                                         (utils/http-proxy)))
            token (get-in resp [:headers "X-Subject-Token"])]
        (assoc (get-in resp [:body :token]) :token token)))))

(defmacro defres [name url singular plural & more]
  `(utils/defres ~name "identity" ~url ~singular ~plural ~@more))

(defn services-list [token]
  (->> token :catalog (map :type)))

(defres project "/projects" :project :projects)

(defres region "/regions" :region :regions)

(defres role "/roles" :role :roles)

(defres domain "/domains" :domain :domains)

(defres policy "/policies" :policy :policies)

(defres user "/users" :user :users)

(defn role-assignment-list [token query]
  (-> (utils/request token :get (str (utils/endpoint-get token "identity") "/role_assignments")
                     :query (rename-keys query {:project-id :scope.project.id
                                                :user-id :user.id
                                                :group-id :group.id
                                                :role-id :role.id
                                                :domain-id :scope.domain.id}))
      :body :role_assignments))

(defn role-map [token]
  (let [roles (role-list token)]
    (into {} (for [{name :name id :id} roles]
               [(keyword name) id]))))

(defres user-project "/users/:id/projects" :project :projects :only [list])

(defres role-imply "/roles/:id/roles" :role_inference :role_inference :custom-actions
  [[create :put "/:role"]])

(defres role-inference "/role_inferences" nil :role_inferences :only [list])
