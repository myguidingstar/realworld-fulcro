(ns conduit.ui.components
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.ui.form-state :as fs]
    [conduit.handler.mutations :as mutations]
    #?(:cljs [fulcro.client.mutations :as m :refer [defmutation]])
    #?(:cljs [fulcro.client.data-fetch :as df])
    [fulcro.client.routing :as r]
    #?(:cljs [fulcro.client.dom :as dom] :clj [fulcro.client.dom-server :as dom])))

(declare SettingsForm)

#?(:cljs
   (defn go-to-settings [this]
     (prim/transact! this `[(use-settings-as-form {:user/id 19})
                            (r/route-to {:handler :screen/settings})])))

#?(:cljs
   (defn log-in [this]
     (prim/transact! this `[(mutations/login {:email "foobar@yep.com" :password "foobar"})])))

(defsc NavBar [this _]
  (dom/nav :.navbar.navbar-light
    (dom/div :.container
      (dom/div :.navbar-brand
        "conduit")
      (dom/ul :.nav.navbar-nav.pull-xs-right
        (dom/li :.nav-item
          (dom/div :.nav-link.active
            #?(:cljs {:onClick #(prim/transact! this `[(r/route-to {:handler :screen/home})])})
            "Home") )
        (dom/li :.nav-item
          (dom/a :.nav-link
            #?(:cljs {:onClick #(prim/transact! this `[(r/route-to {:handler :screen/editor})])})
            (dom/i :.ion-compose)
            "New Post") )
        (dom/li :.nav-item
          (dom/div :.nav-link
            #?(:cljs {:onClick #(go-to-settings this)})
            (dom/i :.ion-gear-a)
            "Settings"))
        (dom/li :.nav-item
          (dom/div :.nav-link
            #?(:cljs {:onClick #(log-in this)})
            (dom/i :.ion-gear-a)
            "Login"))
        (dom/li :.nav-item
          (dom/div :.nav-link
            #?(:cljs {:onClick #(prim/transact! this `[(load-settings {})])})
            (dom/i :.ion-gear-a)
            "Load settings"))
        (dom/li :.nav-item
          (dom/div :.nav-link
            #?(:cljs {:onClick #(prim/transact! this `[(r/route-to {:handler :screen/sign-up})])})
            "Sign up"))))))

(def ui-nav-bar (prim/factory NavBar))

(defsc Footer [this _]
  (dom/footer
    (dom/div :.container
      (dom/div :.logo-font "conduit")
      (dom/span :.attribution
        "An interactive learning project from "
        (dom/a {:href "https://thinkster.io"} "Thinkster")
        ". Code &amp; design licensed under MIT."))))

(def ui-footer (prim/factory Footer))

(defsc Banner [this _]
  (dom/div :.banner
    (dom/div :.container
      (dom/h1 :.logo-font "conduit")
      (dom/p {} "A place to show off your tech stack."))))

(def ui-banner (prim/factory Banner))

(defsc UserPreview [this {:user/keys [username]}]
  {:query [:user/id :user/username]
   :ident [:user/by-id :user/id]})

(defsc ArticlePreviewMeta [this {:article/keys [author created-at liked-by-count]}]
  {:query [:article/id :article/created-at {:article/liked-by-count [:agg/count]}
           {:article/author (prim/get-query UserPreview)}]
   :ident [:article/by-id :article/id]}
  (dom/div :.article-meta
    (dom/a {:href (str "/users/" (:user/username author))}
      (dom/img {:src (:user/image author)}))
    (dom/div :.info
      (dom/a :.author {:href (str "/users/" (:user/username author))}
        (:user/username author))
      (dom/span :.date
        #?(:clj  created-at
           :cljs (when (instance? js/Date created-at)
                   (.toDateString created-at)))))
    (dom/button :.btn.btn-outline-primary.btn-sm.pull-xs-right
      (dom/i :.ion-heart)
      (:agg/count liked-by-count))))

(def ui-article-preview-meta (prim/factory ArticlePreviewMeta {:keyfn :article/id}))

(defsc ArticlePreview [this {:article/keys [slug title description] :keys [ph/article]}]
  {:ident [:article/by-id :article/id]
   :query [:article/id :article/slug :article/title :article/description :article/body
           {:ph/article (prim/get-query ArticlePreviewMeta)}]}
  (dom/div :.article-preview
    (ui-article-preview-meta article)
    (dom/a :.preview-link {:href (str "/articles/" slug)}
      (dom/h1 {} title)
      (dom/p {} description)
      (dom/span {} "Read more..."))))

(def ui-article-preview (prim/factory ArticlePreview {:keyfn :article/id}))

(defsc Tag [this {:tag/keys [tag]}]
  {:query [:tag/tag :tag/count]}
  (dom/a  :.tag-pill.tag-default {:href (str "/tag/" tag)} tag))

(def ui-tag (prim/factory Tag {:keyfn :tag/tag}))

(defsc Tags [this tags]
  (dom/div :.col-md-3
    (dom/div :.sidebar
      (dom/p {} "Popular Tags")
      (dom/div :.tag-list
        (mapv ui-tag tags)))))

(def ui-tags (prim/factory Tags))

(defsc PersonalFeed [this {:keys [screen] articles :articles/feed}]
  {:initial-state {:screen :screen.feed/personal}
   :ident         (fn [] [screen :top])
   :query         [:screen
                   {[:articles/feed '_] (prim/get-query ArticlePreview)}]}
  (dom/div
    (if (seq articles)
      (mapv ui-article-preview articles)
      "You have no article")))

(defsc GlobalFeed [this {:keys [screen] articles :articles/all}]
  {:initial-state {:screen :screen.feed/global}
   :ident         (fn [] [screen :top])
   :query         [:screen
                   {[:articles/all '_] (prim/get-query ArticlePreview)}]}
  (dom/div
    (if (seq articles)
      (mapv ui-article-preview articles)
      "No article")))

(r/defrouter FeedsRouter :router/feeds
  (fn [this props] [(:screen props) :top])
  :screen.feed/personal PersonalFeed
  :screen.feed/global   GlobalFeed)

(def ui-feeds-router (prim/factory FeedsRouter))

(defn select-feeds [this]
  (dom/div :.feed-toggle
    (dom/ul :.nav.nav-pills.outline-active
      (dom/li :.nav-item
        (dom/div :.nav-link.disabled
          {:onClick #(prim/transact! this `[(r/route-to {:handler :screen.feed/personal})])}
          "Your Feed"))
      (dom/li :.nav-item
        (dom/div :.nav-link.active
          {:onClick #(prim/transact! this `[(r/route-to {:handler :screen.feed/global})])}
          "Global Feed")))))

(defsc Home [this {tags   :tags/all
                   router :router/feeds}]
  {:initial-state (fn [params] {:screen       :screen/home
                                :screen-id    :top
                                :router/feeds (prim/get-initial-state FeedsRouter {})})

   :query         [:screen :screen-id
                   {:router/feeds (prim/get-query FeedsRouter)}
                   {[:tags/all '_] (prim/get-query Tag)}]}
  (dom/div :.home-page
    (ui-banner)
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-9
          (select-feeds this)
          (ui-feeds-router router))
        (ui-tags tags)))))

(defsc Profile [this {:user/keys [id username photo bio like]}]
  {:ident [:user/by-id :user/id]
   :query [:user/id :user/username :user/photo :user/bio
           {:user/like (prim/get-query ArticlePreview)}]}
  (dom/div :.profile-page
    ;;(dom/div {:onClick #(df/load this [:user/by-id 19] Profile)} "update this person")
    (dom/div :.user-info
      (dom/div :.container
        (dom/div :.row
          (dom/div :.col-xs-12.col-md-10.offset-md-1
            (dom/img :.user-img {:src photo})
            (dom/h4 {} username)
            (dom/p {} bio)
            (dom/button :.btn.btn-sm.btn-outline-secondary.action-btn
              (dom/i :.ion-plus-round) (str "Follow " username))))))
    (dom/div :.container
      (dom/div :.row
        (dom/div :.col-xs-12.col-md-10.offset-md-1
          (dom/div :.articles-toggle
            (dom/ul :.nav.nav-pills.outline-active
              (dom/li :.nav-item
                (dom/a :.nav-link.active {:href ""}
                  "My Articles"))
              (dom/li :.nav-item
                (dom/a :.nav-link {:href ""}
                  "Favorited Articles"))))
          (mapv ui-article-preview like))))))

(def ui-profile (prim/factory Profile))

(declare ArticleEditor)

#?(:cljs
   (defmutation use-article-as-form [{:article/keys [id]}]
     (action [{:keys [state]}]
       (swap! state #(-> %
                       (fs/add-form-config* ArticleEditor [:article/by-id id])
                       (assoc-in [:root/article-editor :article-to-edit] [:article/by-id id]))))))

(defsc ArticleEditor [this {:article/keys [id slug title description body] :as props}]
  {:query       [:article/id :article/slug  :article/title :article/description :article/body
                 fs/form-config-join]
   :ident       [:article/by-id :article/id]
   :form-fields #{:article/slug  :article/title
                  :article/description :article/body}}
  (dom/div :.editor-page
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-10.offset-md-1.col-xs-12
          (dom/form {}
            (dom/fieldset {}
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Article Title",
                   :type        "text"
                   :value       title
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :article/title})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :article/title :event %))}))
              (dom/fieldset :.form-group
                (dom/input :.form-control
                  {:placeholder "What's this article about?",
                   :type        "text"
                   :value       description
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :article/description})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :article/description :event %))}))
              (dom/fieldset :.form-group
                (dom/textarea :.form-control
                  {:rows  "8", :placeholder "Write your article (in markdown)"
                   :value body
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :article/body})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :article/body :event %))}))
              (dom/fieldset :.form-group
                (dom/input :.form-control
                  {:placeholder "Enter tags",
                   :type        "text"})
                (dom/div :.tag-list))
              (dom/button :.btn.btn-lg.pull-xs-right.btn-primary
                {:type "button"
                 :onClick
                 #?(:clj  nil
                    :cljs #(prim/transact! this `[(mutations/submit-article ~{:article/id id :diff (fs/dirty-fields props false)})]))}
                "Publish Article"))))))))

(def ui-article-editor (prim/factory ArticleEditor))

(defsc Settings [this props]
  {:initial-state (fn [params] {})
   :query         [:user/photo :user/name :user/bio :user/email]})

#?(:cljs
   (defmutation load-settings [_]
     (action [{:keys [state] :as env}]
       (df/load-action env :user/whoami SettingsForm {:without #{:fulcro.ui.form-state/config}}))
     (remote [env]
       (df/remote-load env))))

#?(:cljs
   (defmutation use-settings-as-form [{:user/keys [id]}]
     (action [{:keys [state] :as env}]
       (swap! state #(-> %
                       (fs/add-form-config* SettingsForm [:user/by-id id])
                       (assoc-in [:root/settings-form :settings] [:user/by-id id]))))))

(defsc SettingsForm [this {:user/keys [id photo name bio email] :as props}]
  {:query       [:user/id :user/photo :user/name :user/bio :user/email
                 fs/form-config-join]
   :ident [:user/by-id :user/id]
   :form-fields #{:user/photo :user/name :user/bio :user/email}}
  (dom/div :.settings-page
    (dom/div :.container.page
      (dom/div :.row
        (dom/div :.col-md-6.offset-md-3.col-xs-12
          (dom/h1 :.text-xs-center
            "Your Settings")
          (dom/form {}
            (dom/fieldset {}
              (dom/fieldset :.form-group
                (dom/input :.form-control
                  {:placeholder "URL of profile picture",
                   :type        "text"
                   :value       photo
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :user/photo})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :user/photo :event %))}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Your Name",
                   :type        "text"
                   :value       name
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :user/name})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :user/name :event %))}))
              (dom/fieldset :.form-group
                (dom/textarea :.form-control.form-control-lg
                  {:rows        "8",
                   :placeholder "Short bio about you"
                   :value       (or bio "")
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :user/bio})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :user/bio :event %))}))
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Email",
                   :type        "text"
                   :value       email
                   :onBlur
                   #?(:clj  nil
                      :cljs #(prim/transact! this
                               `[(fs/mark-complete! {:field :user/email})]))
                   :onChange
                   #?(:clj nil
                      :cljs #(m/set-string! this :user/email :event %))}))
              #_
              (dom/fieldset :.form-group
                (dom/input :.form-control.form-control-lg
                  {:placeholder "Password",
                   :type        "password"}))
              (dom/button :.btn.btn-lg.btn-primary.pull-xs-right
                {:onClick
                 #?(:clj  nil
                    :cljs #(prim/transact! this `[(mutations/submit-settings ~(fs/dirty-fields props false))]))}
                "Update Settings"))))))))

(def ui-settings-form (prim/factory SettingsForm))