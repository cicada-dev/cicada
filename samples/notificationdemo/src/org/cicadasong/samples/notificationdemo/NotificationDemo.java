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

package org.cicadasong.samples.notificationdemo;

import org.cicadasong.cicadalib.CicadaNotification;
import org.cicadasong.cicadalib.CicadaNotificationManager;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class NotificationDemo extends Activity {
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    final EditText basicTextField = (EditText) findViewById(R.id.basic_text_field);
    Button basicTextButton = (Button) findViewById(R.id.basic_text_button);
    basicTextButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View button) {
        CicadaNotification note = CicadaNotification.createWithText(
            NotificationDemo.this, basicTextField.getText().toString());
        CicadaNotificationManager.startNotification(NotificationDemo.this, note);
      }
    });
  }
}