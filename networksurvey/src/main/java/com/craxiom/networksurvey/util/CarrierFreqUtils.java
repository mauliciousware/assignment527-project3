/*
 * Copyright (C) 2016-2019 Sean J. Barbeau
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
package com.craxiom.networksurvey.util;

import android.location.GnssAntennaInfo;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.craxiom.networksurvey.model.GnssType;
import com.craxiom.networksurvey.model.SatelliteStatus;

/**
 * Originally from the GPS Test open source Android app.  https://github.com/barbeau/gpstest
 */
public class CarrierFreqUtils
{

    /**
     * An unknown carrier frequency that doesn't match any known frequencies
     */
    public static String CF_UNKNOWN = "unknown";
    /**
     * Carrier frequencies aren't supported by the device for this signal
     */
    public static String CF_UNSUPPORTED = "unsupported";

    static final double CF_TOLERANCE_MHZ = 1d;

    /**
     * Returns the label that should be displayed for a given GNSS constellation, svid, and carrier
     * frequency in MHz, or null if no carrier frequency label is found
     *
     * @param status Satellite signal to get the carrier frequency label for
     * @return the label that should be displayed for a given GNSS constellation, svid, and carrier
     * frequency in MHz, "unsupported" if CF aren't supported on this device, or "unknown" if no carrier frequency label is found
     */
    public static String getCarrierFrequencyLabel(SatelliteStatus status)
    {
        if (!SatelliteUtils.isCfSupported() || !status.getHasCarrierFrequency())
        {
            return CF_UNSUPPORTED;
        }
        double cfMhz = MathUtils.toMhz(status.getCarrierFrequencyHz());
        int svid = status.getSvid();

        switch (status.getGnssType())
        {
            case NAVSTAR:
                return getNavstarCF(cfMhz);
            case GLONASS:
                return getGlonassCf(cfMhz);
            case BEIDOU:
                return getBeidoucCf(cfMhz);
            case QZSS:
                return getQzssCf(cfMhz);
            case GALILEO:
                return getGalileoCf(cfMhz);
            case IRNSS:
                return getIrnssCf(cfMhz);
            case SBAS:
                return getSbasCf(svid, cfMhz);
            case UNKNOWN:
                break;
            default:
                break;
        }
        // Unknown carrier frequency for given constellation and svid
        return CF_UNKNOWN;
    }

    /**
     * Returns the label that should be displayed for a given GNSS constellation, svid, and carrier
     * frequency in MHz, or null if no carrier frequency label is found
     *
     * @param gnssType            constellation type defined in GnssType
     * @param svid                identification number provided by the GnssStatus.getSvid() method
     * @param carrierFrequencyMhz carrier frequency for the signal in MHz
     * @return the label that should be displayed for a given GNSS constellation, svid, and carrier
     * frequency in MHz or {@link #CF_UNKNOWN} if no carrier frequency label is found.
     */
    public static String getCarrierFrequencyLabel(GnssType gnssType, int svid, float carrierFrequencyMhz)
    {
        if (!SatelliteUtils.isCfSupported())
        {
            return CF_UNSUPPORTED;
        }

        switch (gnssType)
        {
            case NAVSTAR:
                return getNavstarCF(carrierFrequencyMhz);
            case GLONASS:
                return getGlonassCf(carrierFrequencyMhz);
            case BEIDOU:
                return getBeidoucCf(carrierFrequencyMhz);
            case QZSS:
                return getQzssCf(carrierFrequencyMhz);
            case GALILEO:
                return getGalileoCf(carrierFrequencyMhz);
            case IRNSS:
                return getIrnssCf(carrierFrequencyMhz);
            case SBAS:
                return getSbasCf(svid, carrierFrequencyMhz);
            case UNKNOWN:
                break;
            default:
                break;
        }
        // Unknown carrier frequency for given constellation and svid
        return CF_UNKNOWN;
    }

    /**
     * Returns carrier frequency labels for the U.S. GPS NAVSTAR carrier frequencies
     *
     * @param carrierFrequencyMhz carrier frequency in MHz
     * @return carrier frequency label
     */
    public static String getNavstarCF(double carrierFrequencyMhz)
    {
        if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42, CF_TOLERANCE_MHZ))
        {
            return "L1";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1227.6, CF_TOLERANCE_MHZ))
        {
            return "L2";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1381.05, CF_TOLERANCE_MHZ))
        {
            return "L3";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1379.913, CF_TOLERANCE_MHZ))
        {
            return "L4";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45, CF_TOLERANCE_MHZ))
        {
            return "L5";
        } else
        {
            return CF_UNKNOWN;
        }
    }

    /**
     * Returns carrier frequency labels for the Russia GLONASS carrier frequencies
     *
     * @param carrierFrequencyMhz carrier frequency in MHz
     * @return carrier frequency label
     */
    public static String getGlonassCf(double carrierFrequencyMhz)
    {
        if (carrierFrequencyMhz >= 1598.0 && carrierFrequencyMhz <= 1606.0)
        {
            // Actual range is 1598.0625 MHz to 1605.375, but allow padding for float comparisons - #103
            return "L1";
        } else if (carrierFrequencyMhz >= 1242.0 && carrierFrequencyMhz <= 1249.0)
        {
            // Actual range is 1242.9375 MHz to 1248.625, but allow padding for float comparisons - #103
            return "L2";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1207.14, CF_TOLERANCE_MHZ))
        {
            // Exact range is unclear - appears to be 1202.025 - 1207.14 - #103
            return "L3";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45, CF_TOLERANCE_MHZ))
        {
            return "L5";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42, CF_TOLERANCE_MHZ))
        {
            return "L1-C";
        } else
        {
            return CF_UNKNOWN;
        }
    }

    /**
     * Returns carrier frequency labels for the Chinese Beidou carrier frequencies
     *
     * @param carrierFrequencyMhz carrier frequency in MHz
     * @return carrier frequency label
     */
    public static String getBeidoucCf(double carrierFrequencyMhz)
    {
        if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1561.098, CF_TOLERANCE_MHZ))
        {
            return "B1I";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42, CF_TOLERANCE_MHZ))
        {
            return "B1C";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45, CF_TOLERANCE_MHZ))
        {
            return "B2a";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1207.14, CF_TOLERANCE_MHZ))
        {
            return "B2b";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1268.52, CF_TOLERANCE_MHZ))
        {
            return "B3I";
        } else
        {
            return CF_UNKNOWN;
        }
    }

    /**
     * Returns carrier frequency labels for the Japanese QZSS carrier frequencies
     *
     * @param carrierFrequencyMhz carrier frequency in MHz
     * @return carrier frequency label
     */
    public static String getQzssCf(double carrierFrequencyMhz)
    {
        if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42, CF_TOLERANCE_MHZ))
        {
            return "L1";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1227.6, CF_TOLERANCE_MHZ))
        {
            return "L2";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45, CF_TOLERANCE_MHZ))
        {
            return "L5";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1278.75, CF_TOLERANCE_MHZ))
        {
            return "L6";
        } else
        {
            return CF_UNKNOWN;
        }
    }

    /**
     * Returns carrier frequency labels for the EU Galileo carrier frequencies
     *
     * @param carrierFrequencyMhz carrier frequency in MHz
     * @return carrier frequency label
     */
    public static String getGalileoCf(double carrierFrequencyMhz)
    {
        if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42, CF_TOLERANCE_MHZ))
        {
            return "E1";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1191.795, CF_TOLERANCE_MHZ))
        {
            return "E5";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45, CF_TOLERANCE_MHZ))
        {
            return "E5a";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1207.14, CF_TOLERANCE_MHZ))
        {
            return "E5b";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1278.75, CF_TOLERANCE_MHZ))
        {
            return "E6";
        } else
        {
            return CF_UNKNOWN;
        }
    }

    /**
     * Returns carrier frequency labels for the Indian IRNSS carrier frequencies
     *
     * @param carrierFrequencyMhz carrier frequency in MHz
     * @return carrier frequency label
     */
    public static String getIrnssCf(double carrierFrequencyMhz)
    {
        if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42, CF_TOLERANCE_MHZ))
        {
            return "L1";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45, CF_TOLERANCE_MHZ))
        {
            return "L5";
        } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 2492.028, CF_TOLERANCE_MHZ))
        {
            return "S";
        } else
        {
            return CF_UNKNOWN;
        }
    }

    /**
     * Returns carrier frequency labels for the SBAS carrier frequencies
     *
     * @param svid                the satellite ID
     * @param carrierFrequencyMhz carrier frequency in MHz
     * @return carrier frequency label
     */
    public static String getSbasCf(int svid, double carrierFrequencyMhz)
    {
        if (svid == 121 || svid == 123 || svid == 126 || svid == 136 || svid == 150)
        {
            // EGNOS - https://gssc.esa.int/navipedia/index.php/EGNOS_Space_Segment
            if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42, CF_TOLERANCE_MHZ))
            {
                return "L1";
            } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45, CF_TOLERANCE_MHZ))
            {
                return "L5";
            }
        } else if (svid == 122)
        {
            // SouthPAN (Australia/New Zealand)
            if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42, CF_TOLERANCE_MHZ))
            {
                return "L1";
            } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45, CF_TOLERANCE_MHZ))
            {
                return "L5";
            }
        } else if (svid == 129 || svid == 137 || svid == 139)
        {
            // MSAS (Japan) - https://gssc.esa.int/navipedia/index.php/MSAS_Space_Segment
            if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42, CF_TOLERANCE_MHZ))
            {
                return "L1";
            } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45, CF_TOLERANCE_MHZ))
            {
                return "L5";
            }
        } else if (svid == 127 || svid == 128 || svid == 132)
        {
            // GAGAN (India)
            if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42, CF_TOLERANCE_MHZ))
            {
                return "L1";
            }
        } else if (svid == 131 || svid == 133 || svid == 135 || svid == 138)
        {
            // WAAS (US)
            if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1575.42, CF_TOLERANCE_MHZ))
            {
                return "L1";
            } else if (MathUtils.fuzzyEquals(carrierFrequencyMhz, 1176.45, CF_TOLERANCE_MHZ))
            {
                return "L5";
            }
        } else if (svid == 125 || svid == 140 || svid == 141)
        {
            // SDCM (Russia)
            return getGlonassCf(carrierFrequencyMhz);
        } else if (svid == 130 || svid == 143 || svid == 144)
        {
            // BDSBAS (China)
            return getBeidoucCf(carrierFrequencyMhz);
        }
        return CF_UNKNOWN;
    }

    /**
     * Returns the carrier frequency label (e.g. "L1") for the provided GNSS antenna's
     * carrier frequency. TODO - combine this with getCarrierFrequencyLabel(SatelliteStatus status)?
     *
     * @param gnssAntennaInfo
     * @return the carrier frequency label (e.g. "L1") for the provided GNSS antenna's
     * carrier frequency
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static String getCarrierFrequencyLabel(GnssAntennaInfo gnssAntennaInfo)
    {
        String label;
        double cfMHz = gnssAntennaInfo.getCarrierFrequencyMHz();
        // Try each GNSS until we find a valid label
        label = getNavstarCF(cfMHz);
        if (label.equals(CF_UNKNOWN))
        {
            label = getGalileoCf(cfMHz);
        }
        if (label.equals(CF_UNKNOWN))
        {
            label = getGlonassCf(cfMHz);
        }
        if (label.equals(CF_UNKNOWN))
        {
            label = getBeidoucCf(cfMHz);
        }
        if (label.equals(CF_UNKNOWN))
        {
            label = getQzssCf(cfMHz);
        }
        if (label.equals(CF_UNKNOWN))
        {
            label = getIrnssCf(cfMHz);
        }
        return label;
    }

    /**
     * Returns true if the provided carrier frequency label is a primary carrier frequency (e.g., "L1")
     * (i.e., it is not a secondary frequency such as "L5") or false if it is not a primary carrier
     * frequency
     *
     * @param label carrier frequency label
     * @return true if the provided carrier frequency label is a primary carrier frequency (e.g., "L1")
     * * (i.e., it is not a secondary frequency such as "L5") or false if it is not a primary carrier
     * * frequency
     */
    public static boolean isPrimaryCarrier(String label)
    {
        return label.equals("L1") || label.equals("E1") || label.equals("L1-C") || label.equals("B1I") || label.equals("B1C");
    }
}
