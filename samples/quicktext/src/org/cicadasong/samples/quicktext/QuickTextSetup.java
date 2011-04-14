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

package org.cicadasong.samples.quicktext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class QuickTextSetup extends Activity {
  
  private EditText nameField;
  private EditText numberField;
  private EditText messageField;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    SharedPreferences prefs =
      getSharedPreferences(QuickText.SHARED_PREF_NAME, Context.MODE_PRIVATE);
    
    nameField = (EditText) findViewById(R.id.recipient_name_field);
    String recipientName = prefs.getString(QuickText.PREF_RECIPIENT_NAME, "");
    nameField.setText(recipientName);
    
    numberField = (EditText) findViewById(R.id.recipient_number_field);
    String recipientNumber = prefs.getString(QuickText.PREF_RECIPIENT_NUMBER, "");
    numberField.setText(recipientNumber);
    
    nameField.setText(recipientName);
    
    messageField = (EditText) findViewById(R.id.message_field);
    String message = prefs.getString(QuickText.PREF_MESSAGE, "");
    messageField.setText(message);
    
    Button button = (Button) findViewById(R.id.save_button);
    button.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        saveSettings();
      }
    });
  }
  
  private void saveSettings() {
    SharedPreferences.Editor prefEdit =
      getSharedPreferences(QuickText.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit();
    prefEdit.putString(QuickText.PREF_RECIPIENT_NAME, nameField.getText().toString());
    prefEdit.putString(QuickText.PREF_RECIPIENT_NUMBER, numberField.getText().toString());
    prefEdit.putString(QuickText.PREF_MESSAGE, messageField.getText().toString());
    prefEdit.commit();
    
    sendBroadcast(new Intent(QuickText.ACTION_SETUP_UPDATE));
  }

}
