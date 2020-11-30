(ns conduit.client
  (:require
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.dom :as dom :refer [div button h3 label a input]]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
   [com.fulcrologic.fulcro.ui-state-machines :as uism]
   [conduit.app :refer [APP routing-start! route-to!]]
   [conduit.session :as session :refer [CurrentUser ui-current-user]]
   [conduit.ui.account :as account]
   [conduit.ui.home :as home]
   [com.fulcrologic.fulcro.inspect.preload]
   [com.fulcrologic.fulcro.inspect.dom-picker-preload]))

(defsc Home [this props]
  {:query         [:pretend-data]
   :ident         (fn [] [:component/id :home])
   :route-segment ["home"]
   :initial-state {}}
  (dom/div :.ui.container.segment
    (h3 "Home Screen")))

(defrouter MainRouter [this props]
  {:router-targets [Home account/LoginForm account/SignUpForm account/Settings]})

(def ui-main-router (comp/factory MainRouter))

(defsc Root [_ {:root/keys    [ready? router]
                :session/keys [current-user]}]
  {:query         [:root/ready?
                   {:root/router (comp/get-query MainRouter)}
                   [::uism/asm-id ::session/sessions]
                   {:session/current-user (comp/get-query session/CurrentUser)}]
   :initial-state (fn [_]
                    {:root/ready? false
                     :root/router (comp/get-initial-state MainRouter)
                     :session/current-user (comp/get-initial-state session/CurrentUser)})}
  (let [logged-in? (:user/valid? current-user)]
    (div
      (home/ui-nav-bar {:logged-in? logged-in?
                        :current-user current-user
                        ;; FIXME
                        :current-route []})
      (when ready?
        (ui-main-router router)))))

(defn refresh []
  (app/mount! APP Root "app"))

(defn ^:export start []
  (app/mount! APP Root "app")
  (dr/initialize! APP)
  (routing-start!)
  (uism/begin! APP session/session-machine ::session/sessions
    {:actor/user session/CurrentUser
     :actor/login-form account/LoginForm
     :actor/sign-up-form account/SignUpForm}
    {:desired-path (some-> js/window .-location .-pathname)}))

(start)

(comment
  (refresh))
