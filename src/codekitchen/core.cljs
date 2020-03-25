(ns codekitchen.core
  (:require [rum.core :as rum]
            [clojure.string :as string]
            ["firebase/app" :as firebase]
            ["firebase/database" :as firebase-db]))

(def heading-font "Georgia")
(def text-font "Georgia")

(def fb-config
  #js {:apiKey            "AIzaSyCVy8VkRF-s5lubzXCC7Xrbdnf1ZIInm9g"
       :authDomain        "cooking-with-code-f62d1.firebaseapp.com"
       :databaseURL       "https://cooking-with-code-f62d1.firebaseio.com"
       :projectId         "cooking-with-code-f62d1"
       :storageBucket     "cooking-with-code-f62d1.appspot.com"
       :messagingSenderId "701990296926"
       :appId             "1:701990296926:web:22e3cab67d74a3b52603cf"})

(defonce fb-app (firebase/initializeApp fb-config))
(defonce fb-db (.database fb-app))
(def coll-submissions (.ref fb-db "submissions"))

(defn e->v [e] (.. e -target -value))

(defn server-timestamp []
  ;database.ServerValue.TIMESTAMP
  (.. firebase -database -ServerValue -TIMESTAMP))
  ;(.serverTimestamp (.-FieldValue (.-database firebase))))

(defn submit-form! [data cb eb]
  (-> (.push coll-submissions
             (clj->js (assoc data :submitted-at (server-timestamp))))
      (.then (fn [result] (cb result)))
      (.catch (fn [err] (eb err)))))

(rum/defc field [model & {:keys [type label placeholder validator] :or {placeholder "Text..."
                                                                        validator   (fn [s] true)}}]
  (let [value @model]
    (list
      [:label {:style {:text-align "right"}} label ":"]
      [:input {:type        type
               :value       value
               :placeholder placeholder
               :style       {:padding       "0.62em"
                             :margin        "0.62em"
                             :font-family   text-font
                             :font-size     "1em"
                             :border-radius "0.31em"
                             :border        "1px solid #aaa"
                             :border-color  (if (validator value) "inherit" "red")}
               :on-change   #(reset! model (e->v %))}])))

(defn heading [key opts & body]
  (into [key
         (merge {:style {:font-family heading-font}} (if (map? opts) opts nil))]
        (if (map? opts) body (cons opts body))))

(defn h1 [opts & body] (apply heading :h1 opts body))
(defn h2 [opts & body] (apply heading :h2 opts body))
(defn h3 [opts & body] (apply heading :h3 opts body))

(rum/defcs root-component
  < (rum/local "" ::email)
    (rum/local "" ::name)
    (rum/local false ::loading?)
    (rum/local ::pending ::state)
  [state]
  (let [!email (::email state)
        !name  (::name state)
        !state  (::state state)
        !loading? (::loading state)
        valid? (not (string/blank? @!email))]
    [:div
     {:style {:margin      "0 auto"
              :font-family text-font
              :max-width   "720px"}}

     [:div
      {:style {:text-align "center"}}
      (h1 "👩‍🍳 Cooking With Code 👨‍🍳") ;": " [:small "Cooking With Code"])
      (h2 "Learn to Code Under Lockdown")]
     [:div
      {:style {:display               "grid"
               :grid-column-gap       "1em"
               :grid-template-rows "1fr"
               :grid-template-columns "1fr 1fr"}}
      [:div
       {:style {:font-size   "1.41em"
                :line-height "1.62em"}}
       [:p "Welcome to The Code Kitchen! I'm your host, " [:a {:href "http://petrustheron.com/"} "Petrus Theron"] ", and I'll be teaching programming in 1-on-1 or small group sessions."]
       [:p "We'll start with the fundamentals of Clojure, the "
        [:a {:href "https://insights.stackoverflow.com/survey/2019#top-paying-technologies"} "highest-paid skill"]
        " in the world."]]
      [:form
       {:disabled  (not valid?)
        :on-submit (fn [e]
                     (.preventDefault e)
                     (if valid?
                       (submit-form!
                         {:email @!email :name @!name}
                         #(reset! !state ::submitted)
                         #(reset! !state ::failed))))}
       [:fieldset
        {:style {:background    "#eeeeff"
                 :border-radius "1em"}}
        (h3 {:style {:text-align "center"}} "Join the Course:")
        [:div
         {:style {:display               "grid"
                  :background            "#eeeeff"
                  :border-radius         "1em"
                  :align-items           "center"
                  :grid-template-columns "1fr 1fr"}}
         (field !name :label "Full Name"
                :type :text
                :placeholder "Jamie Oliver")
         (field !email
                :label "Email"
                :type :email
                :placeholder "jamie@example.com"
                :validator (fn [s] (or (string/blank? s) (re-matches #".+@.+" s))))
         [:div]                                             ;; spacer
         (case @!state
           ::submitted [:p "Great! I look forward to seeing what you'll build :)"
                        ;[:button {:on-click #(reset! !state ::pending)} "Invite "]
                        [:button {:type "button"
                                  :on-click (fn [e]
                                              (.preventDefault e)
                                              (reset! !email "")
                                              (reset! !name "")
                                              (reset! !state ::pending))} "Invite a Friend"]]
           ::failed [:p "Whoops, there seems to have been a problem."
                     [:button {:on-click #(reset! !state ::pending)} "Try Again?"]]
           ::pending [:button {:disabled (not valid?)
                               :style    {
                                          :background    (if valid? "orange")
                                          :color         (if valid? "black")
                                          :border-radius "1em"
                                          :margin        "1em 0"
                                          :padding       "1em 0.62em"}
                               ;:border "1px solid navy"}
                               :type     "submit"} "Sign Me Up!"])]]]]


     [:div
      {:style {:text-align "center"}}
      [:p "© 2020 " [:a {:href "petrustheron.com"} "Petrus Theron"]]
      [:p "This live form was built in 155 lines of ClojureScript with no frameworks or styling dependencies."]]]))

(defn start []
  (rum/mount (root-component) (js/document.getElementById "app")))

(defn ^:export init []
  (start))

(defn stop [])
