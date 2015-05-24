/*
 * Copyright (C) 2015 Zemin Liu
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

package zemin.notification;

/**
 * Utils
 */
public class Utils {

    /**
     * Calculate the alpha value for a position offset.
     *
     * @param alphaStart
     * @param alphaEnd
     * @param posStart
     * @param posEnd
     * @param posOffset
     */
    public static float getAlphaForOffset(float alphaStart, float alphaEnd,
                                          float posStart, float posEnd, float posOffset) {
        return alphaStart + posOffset * (alphaEnd - alphaStart) / (posEnd - posStart);
    }
}
