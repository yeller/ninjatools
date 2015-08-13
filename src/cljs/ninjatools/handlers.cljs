;;;; Copyright © 2015 Carousel Apps, Ltd. All rights reserved.

(ns ninjatools.handlers
  (:require [re-frame.core :as re-frame]
            [ninjatools.db :as db]
            [ajax.core :as ajax]
            [ninjatools.util :refer [log]]
            [clojure.walk]))

(defn report-unexpected-error [{:keys [status status-text]}]
  (js/alert "We are sorry, there was an unexpected error.")
  (log "Error: " status status-text))

(re-frame/register-handler
  :initialize-db
  (fn [_ _]
    {:tools {:data    {}
             :by-slug {}}}))

(re-frame/register-handler
  :display-page-about
  (fn [db [_ _]]
    (assoc db :active-panel :about-panel)))

(re-frame/register-handler
  :display-page-tools
  (fn [db [_ _]]
    (when (empty? (get-in db [:tools :data]))
      (re-frame/dispatch [:get-tools]))
    (assoc db :active-panel :tools-panel)))

(re-frame/register-handler
  :display-page-tool
  (fn [db [_ args]]
    (re-frame/dispatch [:get-tool-with-integrations (:slug args)])
    (-> db
        (assoc :active-panel :tool-panel)
        (assoc :current-tool-slug (:slug args)))))

(re-frame/register-handler
  :get-tools
  (fn [db [_]]
    (ajax/GET "/api/v1/tools"
              {:handler       #(re-frame/dispatch [:got-tools %1])
               :error-handler report-unexpected-error})
    db))

(re-frame/register-handler
  :got-tools
  (fn [db [_ tools]]
    (let [tools (map clojure.walk/keywordize-keys tools)]
      (assoc db :tools {:data    (reduce #(assoc %1 (:id %2) %2) {} tools)
                        :by-slug (reduce #(assoc %1 (:slug %2) (:id %2)) {} tools)}))))


(re-frame/register-handler
  :get-tool-with-integrations
  (fn [db [_ tool-slug tool-requested]]
    (if-let [tool (db/get-tool-by-slug db tool-slug)]
      (when (empty? (:integration-ids tool))
        (ajax/GET (str "/api/v1/tools/" (:id tool) "/integrations")
                  {:handler       #(re-frame/dispatch [:got-integrations (:id tool) %1])
                   :error-handler report-unexpected-error}))
      (do (when (not tool-requested)
            (re-frame/dispatch [:get-tools]))               ; TODO: only get the tool we want, by slug.
          (re-frame/dispatch [:get-tool-with-integrations tool-slug true])))
    db))

(re-frame/register-handler
  :got-integrations
  (fn [db [_ tool-id integration-ids]]
    (let [tool (assoc (get-in db [:tools :data tool-id]) :integration-ids integration-ids)]
      (assoc-in db [:tools :data tool-id] tool))))          ; TODO: get the tools that we have integration ids for when we stop getting all the tools all the time.