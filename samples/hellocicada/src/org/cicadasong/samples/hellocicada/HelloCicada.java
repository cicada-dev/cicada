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
// limitations under the License.

package org.cicadasong.samples.hellocicada;

import org.cicadasong.apollo.ApolloConfig.Button;
import org.cicadasong.apollo.ApolloIntents;
import org.cicadasong.apollo.ApolloIntents.ButtonPress;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.github.cicada.samples.hellocicada.R;

/**
 * A basic class demonstrating how to use ApolloLib to control the device from the idle screen.
 */

public class HelloCicada extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    // Check that we have the right intent & button
    ButtonPress event = ButtonPress.parseIntent(intent);
    if ((event == null) || !event.hasOnlyButtonsPressed(Button.BOTTOM_RIGHT)) {
      return;  // Not yours!
    }
    
    Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.cicada);
    ApolloIntents.pushBitmap(context, bitmap);
    ApolloIntents.vibrate(context, 500, 200, 3);
  }
}