;;;; Copyright © 2015 Carousel Apps, Ltd. All rights reserved.

(ns ^:figwheel-no-load ninjatools.app
  (:require [ninjatools.core :as core]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3449/figwheel-ws"
  :jsload-callback core/mount-root)

(core/init)
