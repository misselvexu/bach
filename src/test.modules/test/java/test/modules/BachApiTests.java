/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.modules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Configuration;
import de.sormuras.bach.Project;
import java.lang.System.Logger.Level;
import org.junit.jupiter.api.Test;

class BachApiTests {

  @Test
  void defaults() {
    var bach = new Bach(Configuration.ofSystem().with(Level.OFF), Project.of());
    assertEquals("Bach.java " + Bach.VERSION, bach.toString());
    var text = "text";
    assertSame(text, bach.log(Level.INFO, text));
  }
}