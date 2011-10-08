// Copyright (C) 2011 Cicada contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.package org.cicadasong.cicadalib;

package org.cicadasong.cicadalib;

public class CicadaIntents {
  private CicadaIntents() {
  }
  
  public static final String PACKAGE_PREFIX = "org.cicadasong.cicada.";
  
  public static final String EXTRA_APP_MODE = "mode";
  public static final String EXTRA_APP_NAME = "name";
  public static final String EXTRA_SESSION_ID = "session_id";
  
  public static final String INTENT_PUSH_CANVAS = "org.cicadasong.cicada.PUSH_CANVAS";
  public static final String EXTRA_BUFFER = "buffer";
  
  public static final String INTENT_VIBRATE = PACKAGE_PREFIX + "VIBRATE";
  public static final String EXTRA_VIBRATE_ON_MSEC = "on";
  public static final String EXTRA_VIBRATE_OFF_MSEC = "off";
  public static final String EXTRA_VIBRATE_NUM_CYCLES = "cycles";
  
  public static final String INTENT_START_NOTIFICATION = PACKAGE_PREFIX + "START_NOTIFICATION";
  public static final String EXTRA_NOTIFICATION_ID = "notification_id";  // int
  public static final String EXTRA_NOTIFICATION_BODY = "notification_body";  // string
  public static final String EXTRA_NOTIFICATION_BODY_RECT = "notification_body_rect";  // int[]
  public static final String EXTRA_PACKAGE_NAME = "package_name";  // string
  public static final String EXTRA_CLASS_NAME = "class_name";  // string
  
  public static final String INTENT_STOP_NOTIFICATION = PACKAGE_PREFIX + "STOP_NOTIFICATION";
}
