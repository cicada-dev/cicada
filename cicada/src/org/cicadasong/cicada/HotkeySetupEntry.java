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
// limitations under the License.package org.cicadasong.cicada;

package org.cicadasong.cicada;

/**
 * This class contains a simple association between a button or buttons (as represented by
 * a bitmask in the "hotkeys" field) and an app.
 */
public class HotkeySetupEntry {
  public final AppDescription app;
  public final byte hotkeys;
  
  public HotkeySetupEntry(AppDescription app, byte hotkeys) {
    this.app = app;
    this.hotkeys = hotkeys;
  }
}
