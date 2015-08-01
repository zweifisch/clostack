(ns clostack.keystone
  (:require [clj-http.client :as client]
            [clojure.set :refer [rename-keys]]
            [clostack.utils :as utils]))

(defn token-create [url &{:keys [domain-name name password scope] :or {domain-name "default"}}]
  (let [body
        {:auth (merge {:identity
                       {:methods ["password"]
                        :password {:user {:domain {:name domain-name}
                                          :name name
                                          :password password}}}}
                      (when scope {:scope scope}))}
        resp (client/post url {:form-params body
                               :content-type :json
                               :accept :json
                               :as :json})
        token (get-in resp [:headers "X-Subject-Token"])]
    (assoc (get-in resp [:body :token]) :token token)))

(defmacro defres [name url singular plural]
  `(utils/defres ~name "identity" ~url ~singular ~plural))

(defn services-list [token]
  (->> token :catalog (map :type)))

(defres project "/projects" :project :projects)

(defres region "/regions" :region :regions)

(defres role "/roles" :role :roles)

(defres domain "/domains" :domain :domains)

(defres policy "/policies" :policy :policies)

(defres user "/users" :user :users)

(defn role-assignment-list [token query]
  (-> (utils/request token :get "/role_assignments"
                     :query (rename-keys query {:project-id :scope.project.id
                                                :user-id :user.id
                                                :group-id :group.id
                                                :role-id :role.id
                                                :domain-id :scope.domain.id}))
      :body))

(defn role-map [token]
  (let [roles (role-list token)]
    (into {} (for [{name :name id :id} roles]
               [(keyword name) id]))))
