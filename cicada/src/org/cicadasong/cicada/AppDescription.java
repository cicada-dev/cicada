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

import org.cicadasong.cicadalib.CicadaApp.AppType;

/**
 * Class used by Cicada to keep track of launchable apps.
 */

public class AppDescription {
  public final String packageName;
  public final String className;
  public final String appName;
  public final AppType modes;

  public AppDescription(String packageName, String className, String appName, AppType modes) {
    this.packageName = packageName;
    this.className = className;
    this.appName = appName;
    this.modes = modes;
  }
  
  public String toString() {
    return appName;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    
    if (this == o) {
      return true;
    }
    
    AppDescription other = (AppDescription) o;
    
    return packageName.equals(other.packageName) && className.equals(other.className) &&
        appName.equals(other.appName) && modes.equals(other.modes);
  }

  @Override
  public int hashCode() {
    int hash = 1;
    hash += 31 * packageName.hashCode();
    hash += 31 * className.hashCode();
    hash += 31 * appName.hashCode();
    hash += 31 * modes.hashCode();
    return hash;
  }
  
}
