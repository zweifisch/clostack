(ns clostack.neutron
  (:require [clostack.utils :as utils]))

(defmacro defres [name url singular plural & more]
  `(utils/defres ~name "network" ~url ~singular ~plural ~@more))

(defres network "/v2.0/networks" :network :networks)

(defres subnet "/v2.0/subnets" :subnet :subnets)

(defres router "/v2.0/routers" :router :routers
  [interface-add :put "/add_router_interface"]
  [interface-remove :put "/remove_router_interface"])

(defres port "/v2.0/ports" :port :ports)

(defres floatingip "/v2.0/floatingips" :floatingip :floatingips)

(defres security-group "/v2.0/security-groups" :security_group :security_groups)

(defres security-group-rule "/v2.0/security-group-rules" :security_group_rule :security_group_rules)

(defres firewall "/v2.0/fw/firewalls" :firewall :firewalls)

(defres firewall-policy "/v2.0/fw/firewall_policies" :firewall_policy :firewall_policies)

;; (defn firewall-policy-update [token id policy]
;;     (-> (request token :put (str "/v2.0/fw/firewall_policies/" id)) :body {:firewall_policy policy}))

(defres firewall-rule "/v2.0/fw/firewall_rules" :firewall_rule :firewall_rules)

(defres extension "/v2.0/extensions" :extension :extensions)

(defres vpn-service "/v2.0/vpn/vpnservices" :vpnservice :vpnservices)

(defres vpn-ikepolicy "/v2.0/vpn/ikepolicies" :ikepolicy :ikepolicies)

(defres vpn-ipsecpolicy "/v2.0/vpn/ipsecpolicies" :ipsecpolicy :ipsecpolicies)

(defres vpn-ipsec-site-connection "/v2.0/vpn/ipsec-site-connections" :ipsec_site_connection :ipsec_site_connections)

(defres lb-vip "/v2.0/lb/vips" :vip :vips)

(defres lb-health-monitor "/v2.0/lb/health_monitors" :health_monitor :health_monitors)

(defres lb-pool "/v2.0/lb/pools" :pool :pools
  [health-monitor-disassociate :delete "/health_monitors"])

(defres lb-member"/v2.0/lb/members" :member :members)

(defres lbaas-loadbalancer "/v2.0/lbaas/loadbalancers" :loadbalancer :loadbalancers)

(defn firewall-policy-with-rules-delete [token id]
  (let [rules (-> (firewall-policy-get token id) :firewall_policie :firewall_rules)]
    (firewall-policy-delete token id)
    (doseq [rule rules]
      (firewall-rule-delete token rule))))

(defres quota "/v2.0/quotas" :quota :quotas)
