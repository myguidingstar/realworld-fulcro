(ns conduit.ui.article-preview
  (:require
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [conduit.handler.mutations :as mutations]
   [conduit.ui.other :as other :refer [display-name]]
   [conduit.session :as session]
   [com.fulcrologic.fulcro.dom :as dom]))

(defn ui-article-preview-meta
  [this {:article/keys [author created-at liked-by-count liked-by-me]}
   {:keys [like unlike]}]
  (dom/div :.article-meta
    (dom/a {:href (str "/profile/" (:user/id author))}
      (dom/img {:src (:user/image author other/default-user-image)}))
    (dom/div :.info
      (dom/a :.author {:href (str "/profile/" (:user/id author))}
        (display-name author))
      (dom/span :.date
        (other/js-date->string created-at)))
    (dom/button :.btn.btn-sm.pull-xs-right
      (if liked-by-me
        {:className "btn-primary"
         :onClick #(unlike)}
        {:className "btn-outline-primary"
         :onClick #(like)})
      (dom/i :.ion-heart) " " liked-by-count)))

(defsc ArticlePreview
  [this {:article/keys [id title description can-edit]
         :as article}
   {:keys [on-delete current-user]}]
  {:ident :article/id
   :initial-state (fn [_] {:article/id :none})
   :query [:article/id :article/author-id :article/slug :article/title :article/description :article/body :article/can-edit
           :article/created-at :article/liked-by-count :article/liked-by-me
           {:article/author (comp/get-query other/UserPreview)}]}
  (dom/div :.article-preview
    (let [like #(if (:user/valid? current-user)
                  (comp/transact! this [(mutations/like {:article/id id})])
                  (js/alert "You must log in first"))
          unlike #(comp/transact! this [(mutations/unlike {:article/id id})])]
      (ui-article-preview-meta this article {:like like :unlike unlike}))
    (when can-edit
      (dom/span :.pull-xs-right
        (dom/a {:href (str "/edit/" id)}
          (dom/i :.ion-edit " "))
        (dom/i :.ion-trash-a
          {:onClick #(on-delete {:article/id id})} " ")))
    (dom/a :.preview-link
      {:href (str "/article/" id)}
      (dom/h1 title)
      (dom/p description)
      (dom/span "Read more..."))))

(def ui-article-preview (comp/computed-factory ArticlePreview {:keyfn :article/id}))

(defn ui-article-list [this {:ui/keys [articles empty-message current-user]}]
  (let [delete-article
        (fn [article] (comp/transact! this [(mutations/delete-article article)]))]
    (dom/div
      (if (sequential? articles)
        (mapv #(ui-article-preview % {:on-delete delete-article :current-user current-user})
          articles)
        empty-message))))
