/*
 * Copyright 2012 Ludovic Claude.
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

package org.debian.maven.packager.util;

import org.apache.maven.model.License;
import org.debian.maven.packager.interaction.SimpleQuestion;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class LicensesScanner {

    public Set<String> discoverLicenses(List<License> projectLicenses) {
        Set<String> licenses = new TreeSet<String>();
        for (License license : projectLicenses) {
            String licenseName = license.getName() != null ? license.getName() + " " : "";
            String licenseUrl  = license.getUrl() != null ? license.getUrl() : "";

            if (!recognizeLicense(licenses, licenseName, licenseUrl)) {
                String s = new SimpleQuestion("License " + licenseName + licenseUrl + " was not recognized, " +
                                        "please enter a license name preferably in one of: " + getAvailableLicenses()).ask();
                if (s.length() > 0) {
                    licenses.add(s);
                }
            }
        }

        System.out.println();
        System.out.println("Checking licenses in the upstream sources...");
        LicenseCheckResult licenseResult = new LicenseCheckResult();
        IOUtil.executeProcess(new String[]{"/bin/sh", "-c", "licensecheck `find . -type f`"}, licenseResult);
        for (String license : licenseResult.getLicenses()) {
            if (!recognizeLicense(licenses, license, "")) {
                String s = new SimpleQuestion("License " + license + " was not recognized, " +
                                        "please enter a license name preferably in one of:" + getAvailableLicenses()).ask();
                if (s.length() > 0) {
                    licenses.add(s);
                }
            }
        }

        if (licenses.isEmpty()) {
            String s = new SimpleQuestion("License was not found, please enter a license name preferably in one of:" + getAvailableLicenses()).ask();
            if (s.length() > 0) {
                licenses.add(s);
            }
        }
        return licenses;
    }

    private String getAvailableLicenses() {
        return "Apache-2.0 Artistic BSD-2-clause BSD-3-clause BSD-4-clause ISC CC-BY CC-BY-SA\n"
             + "CC-BY-ND CC-BY-NC CC-BY-NC-SA CC-BY-NC-ND CC0 CDDL CDDL-1.1 CPL Eiffel EPL-1.0 EPL-2.0 Expat\n"
             + "GPL-2 GPL-3 LGPL-2 LGPL-2.1 LGPL-3 GFDL-1.2 GFDL-1.3 GFDL-NIV LPPL MPL-1.1\n"
             + "MPL-2.0 Perl PSF QPL W3C-Software ZLIB Zope";
    }

    boolean recognizeLicense(Set<String> licenses, String licenseName, String licenseUrl) {
        boolean recognized = false;
        licenseName = licenseName.toLowerCase();
        licenseUrl = licenseUrl.toLowerCase();
        if (licenseName.contains("mit ") || licenseUrl.contains("mit-license")) {
            licenses.add("MIT");
            recognized = true;
        } else if (licenseName.matches(".*bsd \\(?2[ -]clause\\)?.*") || licenseUrl.contains("bsd-2-clause")) {
            licenses.add("BSD-2-clause");
            recognized = true;
        } else if (licenseName.matches(".*bsd \\(?3[ -]clause\\)?.*") || licenseUrl.contains("bsd-3-clause")) {
            licenses.add("BSD-3-clause");
            recognized = true;
        } else if (licenseName.matches(".*bsd \\(?4[ -]clause\\)?.*") || licenseUrl.contains("bsd-4-clause")) {
            licenses.add("BSD-4-clause");
            recognized = true;
        } else if (licenseName.contains("bsd ") || licenseUrl.contains("bsd-license")) {
            licenses.add("BSD");
            recognized = true;
        } else if (licenseName.contains("artistic ") || licenseUrl.contains("artistic-license")) {
            licenses.add("Artistic");
            recognized = true;
        } else if (licenseName.contains("apache ") || licenseUrl.contains("apache")) {
            if (licenseName.contains("2.") || licenseUrl.contains("2.")) {
                licenses.add("Apache-2.0");
                recognized = true;
            } else if (licenseName.contains("1.0") || licenseUrl.contains("1.0")) {
                licenses.add("Apache-1.0");
                recognized = true;
            } else if (licenseName.contains("1.1") || licenseUrl.contains("1.1")) {
                licenses.add("Apache-1.1");
                recognized = true;
            }
        } else if (licenseName.contains("eclipse") || licenseName.contains("epl") || licenseUrl.contains("epl")) {
            if (licenseName.contains("1.0") || licenseUrl.contains("v10")) {
                licenses.add("EPL-1.0");
                recognized = true;
            } else if (licenseName.contains("2.0") || licenseUrl.contains("2.0")) {
                licenses.add("EPL-2.0");
                recognized = true;
            }
        } else if (licenseName.contains("lgpl ") || licenseUrl.contains("lgpl")) {
            if (licenseName.contains("2.1") || licenseUrl.contains("2.1")) {
                licenses.add("LGPL-2.1");
                recognized = true;
            } else if (licenseName.contains("2") || licenseUrl.contains("2")) {
                licenses.add("LGPL-2");
                recognized = true;
            } else if (licenseName.contains("3") || licenseUrl.contains("3")) {
                licenses.add("LGPL-2");
                recognized = true;
            }
        } else if (licenseName.contains("gpl ") || licenseUrl.contains("gpl")) {
            if (licenseName.contains("2") || licenseUrl.contains("2")) {
                licenses.add("GPL-2");
                recognized = true;
            } else if (licenseName.contains("3") || licenseUrl.contains("3")) {
                licenses.add("GPL-3");
                recognized = true;
            }
        } else if (licenseName.contains("mpl") || licenseUrl.contains("mpl")) {
            if (licenseName.contains("1.1") || licenseUrl.contains("1.1")) {
                licenses.add("MPL-1.1");
                recognized = true;
            } else if (licenseName.contains("2.0") || licenseUrl.contains("2.0")) {
                licenses.add("MPL-2.0");
                recognized = true;
            }
        } else if (licenseName.contains("cddl") || licenseUrl.contains("cddl")) {
            if (licenseName.contains("1.1") || licenseUrl.contains("1.1")) {
                licenses.add("CDDL-1.1");
                recognized = true;
            } else if (licenseName.contains("1.0") || licenseUrl.contains("1.0")) {
                licenses.add("CDDL");
                recognized = true;
            }
        } else if (licenseUrl.contains("http://creativecommons.org/licenses/by-sa/3.0")) {
            licenses.add("CC-BY-SA-3.0");
            recognized = true;
        } else if (licenseName.contains("generated file")) {
            // ignore the files reported as generated by licensecheck
            recognized = true;
        }
        return recognized;
    }

}
