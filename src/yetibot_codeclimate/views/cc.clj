(ns yetibot-codeclimate.views.cc
  (:require
    [clojure.string :as s]
    [taoensso.timbre :refer [error info warn]]
    [hiccup.page :refer [include-css]]
    [hiccup.core :refer [h]]
    [yetibot-codeclimate.analyze :refer [run-codeclimate! get-analysis!]]
    [hiccup.element :refer [link-to image]]
    [yetibot.core.webapp.views.common :refer [layout]]
    [ring.util.http-response :refer [ok]]
    [hiccup.element :refer [link-to image]]))

(defn render-line [line]
  (h line))

(defn fmt-lines [start-line lines]
  (s/join
    \newline
    (map-indexed (fn [idx line] (str (+ start-line idx) ": " (render-line line))) lines)))

(defn construct-blob-url [base-url owner repo sha path]
  (str base-url "/" owner "/" repo "/blob/" sha "/" path))

(defn render-items [analysis base-url owner repo sha]
  (doall (for [{:keys [lines engine_name content categories description location]} analysis]
           (let [line-number (or (-> location :positions :begin :line)
                                 (-> location :lines :begin))
                 column-number (-> location :positions :begin :column)]
             [:div.cc-item
              [:div.cc-content
               [:div.row
                [:div.col-md-2
                 [:span.label.label-default
                  {:title (:body content)  :data-toggle "tooltip"}
                  engine_name]
                 " "
                 [:span.label.label-warning
                  (s/join " " (map s/lower-case categories))] ]

                [:div.col-md-10
                 [:p description]
                 [:pre
                  (when (and line-number (every? lines [:before :after :line]))
                    (let [{:keys [before after line]} lines
                          before-count (count before)
                          after-count (count after)]
                      [:span
                       (fmt-lines (- line-number before-count) before)
                       \newline
                       [:mark line-number ": " (render-line line) \newline]
                       (fmt-lines (inc line-number) after)]))

                  [:div.file-location
                   (let [path (:path location)
                         path-with-possible-line (str path (when line-number (str "#L" line-number)))]
                     (link-to
                       (construct-blob-url base-url owner repo sha path-with-possible-line)
                       (str
                         path
                         (when line-number (str " line " line-number))
                         (when column-number (str " column " column-number)))))]]]
                     ]]]))))


(defn analysis-msg [analysis]
  (if (coll? analysis)
    [:p (str "Found " (let [c (count analysis)] (str c " " (if (> c 1) "issues" "issue") ".")))]
    [:div.alert.alert-danger.display-4 "Oops, CodeClimate analysis erred. <br> Everything is on fire. 🔥🔥🔥"]))

(defn show-cc [owner repo sha]
  (let [results (get-analysis! owner repo sha)]
    (layout
      "CodeClimate"
      (include-css "/codeclimate/main.css" )
      [:div.main.container-fluid
       [:h1 "Yetibot CodeClimate "]

       (if-let [{:keys [commit-url base-url sha owner repo analysis]} results]

         [:div
          [:h6.repo-info [:small.lead
                          (str owner "/" repo " at ")
                          (link-to commit-url (subs sha 0 7))]]
          (if (empty? analysis)
            [:div.cc-item.success [:div.cc-content "Looks good, no problems detected!"]]
             ;; analysis can be an error message
             [:div
              (analysis-msg analysis)
              (when (coll? analysis)
                (render-items analysis base-url owner repo sha))])]

         ;; file doesn't exist yet
         [:div.cc-item.pending
          [:div.cc-content
           "No analysis found. Refresh at will."]])

       [:iframe
        {:height "30px",
         :width "160px",
         :scrolling "0",
         :frameborder "0",
         :src
         "https://ghbtns.com/github-btn.html?user=devth&repo=yetibot-codeclimate&type=star&count=true&size=large"}]

       [:div.row
        [:div.col-lg-4
         [:p.small "Feedback? "
          (link-to "https://github.com/devth/yetibot-codeclimate/issues"
                   "Open an issue")]
         ]]

       ]


      )))

(defn list-cc [owner repo]
  "Ok")
