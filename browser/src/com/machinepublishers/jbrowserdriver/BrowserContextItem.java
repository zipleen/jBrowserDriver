/*
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2015 Machine Publishers, LLC
 * ops@machinepublishers.com | machinepublishers.com
 * Cincinnati, Ohio, USA
 *
 * You can redistribute this program and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free
 * Software Foundation. Additional permissions or commercial licensing may be
 * available--see LICENSE file or contact Machine Publishers, LLC for details.
 *
 * For general details about how to investigate and report license violations,
 * please see: https://www.gnu.org/licenses/gpl-violation.html
 * and email the author: ops@machinepublishers.com
 * Keep in mind that paying customers have more rights than the AGPL alone offers.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License version 3
 * for more details.
 */
package com.machinepublishers.jbrowserdriver;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.machinepublishers.jbrowserdriver.Util.Sync;
import com.machinepublishers.jbrowserdriver.config.JavaFx;
import com.machinepublishers.jbrowserdriver.config.JavaFxObject;
import com.machinepublishers.jbrowserdriver.config.Settings;
import com.machinepublishers.jbrowserdriver.config.SettingsManager;
import com.sun.javafx.webkit.Accessor;

class BrowserContextItem {
  private static final AtomicLong currentId = new AtomicLong();

  final AtomicReference<com.machinepublishers.jbrowserdriver.Window> window =
      new AtomicReference<com.machinepublishers.jbrowserdriver.Window>();
  final AtomicReference<com.machinepublishers.jbrowserdriver.Navigation> navigation =
      new AtomicReference<com.machinepublishers.jbrowserdriver.Navigation>();
  final AtomicReference<com.machinepublishers.jbrowserdriver.Options> options =
      new AtomicReference<com.machinepublishers.jbrowserdriver.Options>();
  final AtomicReference<com.machinepublishers.jbrowserdriver.Timeouts> timeouts =
      new AtomicReference<com.machinepublishers.jbrowserdriver.Timeouts>();
  final AtomicReference<com.machinepublishers.jbrowserdriver.TargetLocator> targetLocator =
      new AtomicReference<com.machinepublishers.jbrowserdriver.TargetLocator>();
  final AtomicReference<JavaFxObject> stage = new AtomicReference<JavaFxObject>();
  final AtomicReference<JavaFxObject> view = new AtomicReference<JavaFxObject>();
  final AtomicReference<JavaFxObject> engine = new AtomicReference<JavaFxObject>();
  final AtomicReference<Keyboard> keyboard = new AtomicReference<Keyboard>();
  final AtomicReference<Mouse> mouse = new AtomicReference<Mouse>();
  final AtomicReference<Capabilities> capabilities = new AtomicReference<Capabilities>();
  final AtomicReference<Robot> robot = new AtomicReference<Robot>();
  final AtomicInteger statusCode = new AtomicInteger();
  final AtomicReference<Settings> settings = new AtomicReference<Settings>();
  final AtomicBoolean initialized = new AtomicBoolean();
  final Object initLock = new Object();
  final AtomicBoolean pageLoaded = new AtomicBoolean();
  final AtomicLong settingsId = new AtomicLong();
  final AtomicReference<String> windowHandle = new AtomicReference<String>();

  public BrowserContextItem() {
    windowHandle.set(Long.toString(currentId.getAndIncrement()));
  }

  public void init(final JBrowserDriver driver, final BrowserContext context) {
    if (initialized.get()) {
      return;
    }
    synchronized (initLock) {
      if (!initialized.get()) {
        SettingsManager._register(stage, view,
            settings, statusCode);
        engine.set(view.get().call("getEngine"));
        settingsId.set(Long.parseLong(
            engine.get().call("getUserAgent").toString()));
        robot.set(new Robot(stage, settingsId.get()));
        window.set(new com.machinepublishers.jbrowserdriver.Window(
            stage, settingsId.get()));
        timeouts.set(new com.machinepublishers.jbrowserdriver.Timeouts());
        keyboard.set(new Keyboard(robot));
        mouse.set(new Mouse(robot));
        navigation.set(new com.machinepublishers.jbrowserdriver.Navigation(
            new AtomicReference<JBrowserDriver>(driver), view,
            settingsId.get()));
        options.set(new com.machinepublishers.jbrowserdriver.Options(
            window, timeouts));
        targetLocator.set(new com.machinepublishers.jbrowserdriver.TargetLocator());
        capabilities.set(new Capabilities());
        Util.exec(new Sync<Object>() {
          @Override
          public Object perform() {
            JavaFx.getStatic(Accessor.class, settingsId.get()).
                call("getPageFor", view.get().call("getEngine")).
                call("addLoadListenerClient",
                    JavaFx.getNew(DynamicHttpLog.class, settingsId.get(), settingsId.get()));
            engine.get().call("getLoadWorker").call("stateProperty").call("addListener",
                JavaFx.getNew(DynamicStateListener.class, settingsId.get(),
                    pageLoaded));
            engine.get().call("setCreatePopupHandler",
                JavaFx.getNew(DynamicPopupHandler.class, settingsId.get(), driver, context));
            return null;
          }
        }, settingsId.get());
        initialized.set(true);
      }
    }
  }
}
