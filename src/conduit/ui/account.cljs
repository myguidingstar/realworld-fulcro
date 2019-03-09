(ns conduit.ui.account
  (:require
   [fulcro.client.primitives :as prim :refer [defsc]]
   [conduit.ui.other :as other]
   [fulcro.ui.form-state :as fs]
   [conduit.handler.mutations :as mutations]
   [fulcro.client.mutations :as m :refer [defmutation]]
   [fulcro.client.data-fetch :as df]
   [fulcro.client.dom :as dom]
   [conduit.ui.routes :as routes]))

(defsc Settings [this props]
  {:query [:user/image :user/name :user/bio :user/email]})

(defsc SettingsForm [this {:user/keys [id image name bio email password] :as props}]
  {:query       [:user/id :user/image :user/name :user/bio :user/email :user/password
                 fs/form-config-join]
   :ident       [:user/by-id :user/id]
   :form-fields #{:user/image :user/name :user/bio :user/email :user/password}}
  (dom/div :.settings-page
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-6.offset-md-3.col-xs-12
          (dom/h1 :.text-xs-center
            "Your Settings")
          (dom/form {:onSubmit #(do (.preventDefault %) (prim/transact! this `[(mutations/submit-settings ~(fs/dirty-fields props false))]))}
            (dom/fieldset
              (dom/fieldset :.form-group
                (dom/input :.form-control
                  {:placeholder "URL of profile picture",
                   :type        "text"
                   :value       image
                   :onBlur      #(prim/transact! this
                                   `[(fs/mark-complete! {:field :user/image})])
                   :onChange    #(m/set-string! this :user/image :event %)}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Your Name",
                   :type        "text"
                   :value       name
                   :onBlur      #(prim/transact! this
                                   `[(fs/mark-complete! {:field :user/name})])
                   :onChange    #(m/set-string! this :user/name :event %)}))
              (dom/fieldset :.form-group
                (dom/textarea :.form-control.form-control-lg
                  {:rows        "8",
                   :placeholder "Short bio about you"
                   :value       (or bio "")
                   :onBlur      #(prim/transact! this
                                   `[(fs/mark-complete! {:field :user/bio})])
                   :onChange    #(m/set-string! this :user/bio :event %)}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Email",
                   :type        "text"
                   :value       email
                   :onBlur      #(prim/transact! this
                                   `[(fs/mark-complete! {:field :user/email})])
                   :onChange    #(m/set-string! this :user/email :event %)}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Password",
                   :type        "password"
                   :value       (or password "")
                   :onBlur      #(prim/transact! this
                                    `[(fs/mark-complete! {:field :user/password})])
                   :onChange    #(m/set-string! this :user/password :event %)}))
              (dom/button :.btn.btn-lg.btn-primary.pull-xs-right
                {:type "submit" :value "submit"}
                "Update Settings"))))))))

(def ui-settings-form (prim/factory SettingsForm))

(defsc SignUpForm [this {:user/keys [name email] :as props}]
  {:query         [:user/name :user/email fs/form-config-join]
   :initial-state (fn [params] #:user{:name "" :email ""})
   :ident         (fn [] [:root/sign-up-form :new-user])
   :form-fields   #{:user/name :user/email}}
  (let [{:user/keys [password] :as state} (prim/get-state this)
        whoami                     (prim/shared this :user/whoami)
        logged-in?                 (number? (:user/id whoami))]
    (dom/div :.auth-page
      (dom/div :.container.page
        (dom/div :.row
          (if logged-in?
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/p :.text-xs-center
                "You have logged in as " (:user/name whoami)))
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/h1 :.text-xs-center
                "Sign up")
              (dom/p  :.text-xs-center
                (dom/a {:href (routes/to-path {:handler :screen/log-in})}
                  "Have an account?"))
              #_
              (dom/ul :.error-messages
                (dom/li "That email is already taken") )
              (dom/form {:onSubmit #(do (.preventDefault %) (prim/transact! this `[(sign-up ~(merge props state))]))}
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:placeholder "Your Name"
                     :type        "text"
                     :value       name
                     :onBlur      #(prim/transact! this
                                     `[(fs/mark-complete! {:field :user/name})])
                     :onChange    #(m/set-string! this :user/name :event %)}))
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:placeholder "Email"
                     :type        "text"
                     :value       email
                     :onBlur      #(prim/transact! this
                                     `[(fs/mark-complete! {:field :user/email})])
                     :onChange    #(m/set-string! this :user/email :event %)}))
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:placeholder "Password"
                     :type        "password"
                     :value       (or password "")
                     :onChange    #(prim/set-state! this {:user/password (.. % -target -value)})}) )
                (dom/button :.btn.btn-lg.btn-primary.pull-xs-right
                  {:type "submit" :value "submit"}
                  "Sign up")))))))))

(def ui-sign-up-form (prim/factory SignUpForm))

(defmutation load-sign-up-form [_]
  (action [{:keys [state] :as env}]
    (swap! state
      #(fs/add-form-config* % SignUpForm [:root/sign-up-form :new-user])))
  (refresh [env] [:screen]))

(defsc LogInForm [this props]
  (let [{:user/keys [email password] :as credentials} (prim/get-state this)

        whoami     (prim/shared this :user/whoami)
        logged-in? (number? (:user/id whoami))]
    (dom/div :.auth-page
      (dom/div :.container.page
        (dom/div :.row
          (if logged-in?
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/p :.text-xs-center
                "You have logged in as " (:user/name whoami)))
            (dom/div :.col-md-6.offset-md-3.col-xs-12
              (dom/h1 :.text-xs-center
                "Log in")
              (dom/p :.text-xs-center
                (dom/a {:href (routes/to-path {:handler :screen/sign-up})}
                  "Don't have an account?"))
              (dom/form {:onSubmit #(do (.preventDefault %) (prim/transact! this `[(log-in ~credentials)]))}
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:placeholder "Email"
                     :type        "text"
                     :value       (or email "")
                     :onChange    #(prim/update-state! this assoc :user/email (.. % -target -value))}))
                (dom/fieldset :.form-group
                  (dom/input :.form-control.form-control-lg
                    {:placeholder "Password"
                     :type        "password"
                     :value       (or password "")
                     :onChange    #(prim/update-state! this assoc :user/password (.. % -target -value))}) )
                (dom/button :.btn.btn-lg.btn-primary.pull-xs-right
                  {:type "submit" :value "submit"}
                  "Log in")))))))))

(def ui-log-in-form (prim/factory LogInForm))

(defsc SignUpScreen [this {user :new-user}]
  {:initial-state (fn [params] {:screen    :screen/sign-up
                                :screen-id :top
                                :new-user  #:user {:name "" :email ""}})
   :ident         (fn [] [:screen/sign-up :top])
   :query         [:screen :screen-id
                   {:new-user (prim/get-query SignUpForm)}]}
  (ui-sign-up-form user))

(defsc LogInScreen [this props]
  {:initial-state (fn [params] {:screen    :screen/log-in
                                :screen-id :top})
   :ident         (fn [] [:screen/log-in :top])
   :query         [:screen :screen-id]}
  (ui-log-in-form {}))

(defsc SettingScreen [this {user [:root/settings-form :user]}]
  {:initial-state (fn [params] {:screen             :screen/settings
                                :screen-id          :top})
   :query         [:screen :screen-id
                   {[:root/settings-form :user] (prim/get-query SettingsForm)}]}
  (ui-settings-form user))

(defsc LogInSubmission [this props]
  {:query         [:submission/id
                   :submission/status
                   :submission/errors
                   {:submission/result (prim/get-query other/UserTinyPreview)}]
   :ident         (fn [] [:submission/by-id :app/log-in])})

(defmutation log-in [credentials]
  (remote [{:keys [ast state]}]
    (m/returning ast state LogInSubmission)))

(defmutation log-out [_]
  (action [{:keys [state] :as env}]
    (df/load-action env :user/whoami other/UserTinyPreview
      {:params        {:logout true}
       :post-mutation `mutations/rerender-root}))
  (remote [env]
    (df/remote-load env)))

(defmutation sign-up [new-user]
  (action [{:keys [state] :as env}]
    (df/load-action env :user/whoami SettingsForm
      {:params        {:sign-up new-user}
       :without       #{:fulcro.ui.form-state/config :user/password}
       :post-mutation `mutations/rerender-root}))
  (remote [env]
    (df/remote-load env)))

(defmutation use-settings-as-form [_]
  (action [{:keys [state] :as env}]
    (swap! state #(let [id (-> % :user/whoami second)]
                    (-> %
                      (assoc-in [:user/by-id id :user/password] "")
                      (fs/add-form-config* SettingsForm [:user/by-id id])
                      (assoc-in [:root/settings-form :user] [:user/by-id id]))))))
