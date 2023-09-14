/*
 * Copyright 2011 Ludovic Claude.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.debian.maven.plugin;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DirectoryUtilsTest {

    @Test
    public void testRelativePath() {

        assertEquals("../maven-repo/my/test/file.jar", DirectoryUtils.relativePath("/usr/share/java", "/usr/share/maven-repo/my/test/file.jar"));

    }
}
