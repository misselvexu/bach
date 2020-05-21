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

package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SequencerTests {

  @Test
  void arguments() {
    var arguments =
        new Sequencer.Arguments()
            .put("--first-flag")
            .put("-second-flag")
            .put("--key", "value")
            .put("--joined-path", List.of(Path.of("foo/bar"), Path.of("baz")))
            .add("file-1", "file-2", "file-3", '#');
    assertLinesMatch(
        List.of(
            "--first-flag",
            "-second-flag",
            "--key",
            "value",
            "--joined-path",
            "foo.bar.baz",
            "file-1",
            "file-2",
            "file-3",
            "#"),
        List.of(arguments.toStringArray()));
  }
}
