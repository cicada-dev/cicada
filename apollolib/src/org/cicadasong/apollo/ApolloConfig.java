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

package org.cicadasong.apollo;

/**
 * Defines aspects of Apollo.
 */

public class ApolloConfig {
  public static final int DISPLAY_WIDTH = 96;
  public static final int DISPLAY_HEIGHT = 96;
  
  public enum Button {
    TOP_RIGHT    ((byte) 1),
    MIDDLE_RIGHT ((byte) 2),
    BOTTOM_RIGHT ((byte) 4),
    TOP_LEFT     ((byte) 16),
    MIDDLE_LEFT  ((byte) 32),
    BOTTOM_LEFT  ((byte) 64);
    
    private final byte value;
    
    private Button(byte value) {
      this.value = value;
    }
    
    public byte value() {
      return value;
    }
  }
  
  private ApolloConfig() {
  }
}
