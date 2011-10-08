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

import android.content.Context;
import android.content.Intent;

public class CicadaNotificationManager {
  private CicadaNotificationManager() {
  }
  
  public static void startNotification(Context context, CicadaNotification notification) {
    Intent intent = new Intent(CicadaIntents.INTENT_START_NOTIFICATION);
    intent.putExtras(notification.toBundle());
    context.sendBroadcast(intent);
  }
  
  public static void stopNotification(Context context, CicadaNotification notification) {
    Intent intent = new Intent(CicadaIntents.INTENT_STOP_NOTIFICATION);
    intent.putExtras(notification.toBundle());
    context.sendBroadcast(intent);
  }
}
