/*
 * Copyright (C) 2015 The Android Open Source Project
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

/* Don't edit this file!  It is auto-generated by checksum_gen.py. */

package android.support.v8.renderscript;

import java.util.HashMap;
import java.util.Map;

class MD5CheckSums {
    static final String ARM_rsjni = "40dd2154451dc97e247c80422b534f61";
    static final String ARM_RSSupport = "ca07855dc93bf78cfea31c547a45b07b";
    static final String ARM_RSSupportIO = "194614890998c284815ad84d7bfec094";
    static final String ARM_blasV8 = "c060c05402f24053b051537e300bc1bc";

    static final String MIPS_rsjni = "e0c3b9277750e66977094d253ef9ea39";
    static final String MIPS_RSSupport = "11e50615178ca88818694046b3e83c21";
    static final String MIPS_RSSupportIO = "295ece3cf5e61464a3a618f617bfc9b3";
    static final String MIPS_blasV8 = "d5f0353d88c8ed381c8d38c6faff57b3";

    static final String X86_rsjni = "566c2c49e02ceff2fe4c67b26d042186";
    static final String X86_RSSupport = "47166e2fc81049e7c930042b894e8fd6";
    static final String X86_RSSupportIO = "ee8f567c031511ee18c8824ae24fa897";
    static final String X86_blasV8 = "4c6dcc023f86bbc8504b98816d35a416";

    static final String ARM64_rsjni = "00d5001f0850321a709cbb084f79e875";
    static final String ARM64_RSSupport = "65344ee5881d748c6c23b49bb0f7e5a7";
    static final String ARM64_RSSupportIO = "3df26be4e99e81f0701d7bc4a4c1682a";
    static final String ARM64_blasV8 = "30555e6935be660a2900eec2d4731735";

    private static Map<String, String> MD5Ref = new HashMap<>();
    private static boolean isMapInited = false;

    private static void initMD5CheckSums() {
        MD5Ref.put("ARM_rsjni", ARM_rsjni);
        MD5Ref.put("ARM_RSSupport", ARM_RSSupport);
        MD5Ref.put("ARM_RSSupportIO", ARM_RSSupportIO);
        MD5Ref.put("ARM_blasV8", ARM_blasV8);

        MD5Ref.put("MIPS_rsjni", MIPS_rsjni);
        MD5Ref.put("MIPS_RSSupport", MIPS_RSSupport);
        MD5Ref.put("MIPS_RSSupportIO", MIPS_RSSupportIO);
        MD5Ref.put("MIPS_blasV8", MIPS_blasV8);

        MD5Ref.put("X86_rsjni", X86_rsjni);
        MD5Ref.put("X86_RSSupport", X86_RSSupport);
        MD5Ref.put("X86_RSSupportIO", X86_RSSupportIO);
        MD5Ref.put("X86_blasV8", X86_blasV8);

        MD5Ref.put("ARM64_rsjni", ARM64_rsjni);
        MD5Ref.put("ARM64_RSSupport", ARM64_RSSupport);
        MD5Ref.put("ARM64_RSSupportIO", ARM64_RSSupportIO);
        MD5Ref.put("ARM64_blasV8", ARM64_blasV8);
        return;
    }

    static String getMD5CheckSums(String libName) {
        if (!isMapInited) {
            initMD5CheckSums();
            isMapInited = true;
        }
        return MD5Ref.get(libName);
    }
}
