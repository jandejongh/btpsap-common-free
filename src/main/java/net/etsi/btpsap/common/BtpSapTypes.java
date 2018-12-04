/*
 * Copyright 2016 Jan de Jongh, TNO.
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
 *
 */
package net.etsi.btpsap.common;

import java.util.Arrays;

/** Java binding for common data types in a BTP (Basic Transport Protocol) SAP (Service Access Point entity).
 *
 * <p>
 * After ETSI EN 302 636-5-1, V1.2.1 (2014-08).
 * 
 * @author Jan de Jongh, TNO
 * 
 */
public interface BtpSapTypes
{
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // BTP TYPE
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  enum BtpType
  {
    BTP_A,
    BTP_B
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // GN TRANSPORT TYPE
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  enum GnTransportType
  {
    /** Geonetworking Unicast.
     * 
     */
    GN_UC,
    /** Geonetworking Single-Hop Broadcast.
     * 
     */
    GN_SHB,
    /** Geonetworking Topologically-Scoped Broadcast.
     * 
     */
    GN_TSB,
    /** Geonetworking Geobroadcast.
     * 
     */
    GN_GBC,
    /** Geonetworking Anycast.
     * 
     */
    GN_AC
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // GN AREA SHAPE
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  enum GnAreaShape
  {
    CIRCLE,
    RECTANGLE,
    ELLIPSE
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // GN AREA
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  interface GnArea
  {
    
    GnAreaShape getAreaShape ();
    
    double getLatitude ();
    
    double getLongitude ();
    
    int getDistanceA_m ();
    
    int getDistanceB_m ();
    
    int getAngle_degrees ();
    
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // DEFAULT GN AREA
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static class DefaultGnArea
  implements GnArea
  {
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // CONSTRUCTOR(S) / CLONING / FACTORY
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public DefaultGnArea (final GnAreaShape gnAreaShape,
      final double latitude, final double longitude,
      final int distanceA_m, final int distanceB_m,
      final int angle_degrees)
    {
      if (gnAreaShape == null)
        throw new IllegalArgumentException ();
      this.gnAreaShape = gnAreaShape;
      this.latitude = latitude;
      this.longitude = longitude;
      this.distanceA_m = distanceA_m;
      this.distanceB_m = distanceB_m;
      this.angle_degrees = angle_degrees;
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // AREA SHAPE
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private final GnAreaShape gnAreaShape;

    @Override
    public final GnAreaShape getAreaShape ()
    {
      return this.gnAreaShape;
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // LATITUDE
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private final double latitude;

    @Override
    public final double getLatitude ()
    {
      return this.latitude;
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // LONGITUDE
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private final double longitude;

    @Override
    public final double getLongitude ()
    {
      return this.longitude;
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // DISTANCE A
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private final int distanceA_m;

    @Override
    public final int getDistanceA_m ()
    {
      return this.distanceA_m;
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // DISTANCE B
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private final int distanceB_m;
    
    @Override
    public final int getDistanceB_m ()
    {
      return this.distanceB_m;
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // ANGLE
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private final int angle_degrees;

    @Override
    public final int getAngle_degrees ()
    {
      return this.angle_degrees;
    }
    
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // GN ADDRESS
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  interface GnAddress
  {
    
    long toLong ();
    
  }
  
  static class DefaultGnAddress
  implements GnAddress
  {
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // CONSTRUCTOR(S) / CLONING / FACTORY
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public DefaultGnAddress (final byte[] gnAddressBytes)
    {
      if (gnAddressBytes == null || gnAddressBytes.length != 8)
        throw new IllegalArgumentException ();
      this.gnAddressBytes = Arrays.copyOf (gnAddressBytes, 8);
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // GN ADDRESS BYTES
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private final byte [] gnAddressBytes;
    
    public final byte [] getGnAddressBytes ()
    {
      return this.gnAddressBytes;
    }

    @Override
    public final long toLong ()
    {
      long result = 0;
      for (int i = 0; i < 8; i++)
      {
        result <<= 8;
        result |= (this.gnAddressBytes[i] & 0xff);
      }
      return result;
    }
    
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // GN DESTINATION TYPE
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  enum GnDestinationType
  {
    GN_DEST_UC,
    GN_DEST_GBC_AC
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // GN DESTINATION
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  interface GnDestination
  {
    
    GnDestinationType getGnDestinationType ();
    
    GnAddress getGnUnicastAddress () throws IllegalStateException;
    
    GnArea getGnArea () throws IllegalStateException;
    
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // DEFAULT GN DESTINATION
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static class DefaultGnDestination
  implements GnDestination
  {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // CONSTRUCTOR(S) / CLONING / FACTORY
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private DefaultGnDestination (final GnDestinationType gnDestinationType, final GnAddress gnUnicastAddress, final GnArea gnArea)
    {
      if (gnDestinationType == null)
        throw new IllegalArgumentException ();
      if (gnDestinationType == GnDestinationType.GN_DEST_UC && (gnUnicastAddress == null || gnArea != null))
        throw new IllegalArgumentException ();
      if (gnDestinationType == GnDestinationType.GN_DEST_GBC_AC && (gnUnicastAddress != null || gnArea == null))
        throw new IllegalArgumentException ();
      this.gnDestinationType = gnDestinationType;
      this.gnUnicastAddress = gnUnicastAddress;
      this.gnArea = gnArea;
    }

    public DefaultGnDestination (final GnAddress gnUnicastAddress)
    {
      this (GnDestinationType.GN_DEST_UC, gnUnicastAddress, null);
    }

    public DefaultGnDestination (final GnArea gnArea)
    {
      this (GnDestinationType.GN_DEST_GBC_AC, null, gnArea);
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // GN DESTINATION TYPE
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private final GnDestinationType gnDestinationType;
    
    @Override
    public final GnDestinationType getGnDestinationType ()
    {
      return this.gnDestinationType;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // GN UNICAST ADDRESS
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private final GnAddress gnUnicastAddress;
    
    @Override
    public final GnAddress getGnUnicastAddress () throws IllegalStateException
    {
      if (this.gnDestinationType != GnDestinationType.GN_DEST_UC)
        throw new IllegalStateException ();
      return this.gnUnicastAddress;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // GN AREA
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private final GnArea gnArea;
    
    @Override
    public final GnArea getGnArea () throws IllegalStateException
    {
      if (this.gnDestinationType != GnDestinationType.GN_DEST_GBC_AC)
        throw new IllegalStateException ();
      return this.gnArea;
    }
    
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // GN COMMUNICATIONS PROFILE
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  enum GnCommunicationsProfile
  {
    GN_COMPROF_ITSG5,
    GN_COMPROF_CELLULAR;
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // GN SECURITY PROFILE
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /** The security-profile interface.
   * 
   * <p>
   * In the BTP SAP (ETSI EN 302 636-5-1), the security profile is not (well) defined, other than that it
   * <i>"determines the security function to invoke."</i>
   * In addition, the security profile is <i>not</i> part of the SN interface (ETSI TS 102 723-8 V1.1.1 (2016-04)),
   * but service primitives therein contained typically required the ITS AID (e.g., ETSI TS 102 965 V1.3.1 (2016-11)),
   * and SSP (Service-Specific Permissions, e.g., ETSI TS 103 097 V1.2.1 (2015-06)),
   * which are not part of the BTP SAP.
   * 
   * <p>
   * We follow the convention found elsewhere that the ITS-AID serves as a "key" to the proper security profile, in other words,
   * it determines which security features to invoke (and how). The SSP act as additional "arguments".
   * We therefore construct {@link GnSecurityProfile} as the composition of an ITS-AID (value-restricted to a {@code long}),
   * and the SSP (at most 31 octets, cf ETSI TS 103 097 V1.2.1 (2015-06)).
   * 
   */
  interface GnSecurityProfile
  {
    
    long getItsAid ();
    
    GnPermissions getPermissions ();
    
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // BASE [DEFAULT] GN SECURITY PROFILE
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /** A base implementation of {@link GnSecurityProfile}.
   * 
   * <p>
   * Note that we refrain from using the {@code Default} prefix as it leads to confusion with
   * "default profile" in the security context.
   * 
   */
  static class BaseGnSecurityProfile
  implements GnSecurityProfile
  {
    
    private final long itsAid;

    @Override
    public final long getItsAid ()
    {
      return this.itsAid;
    }
    
    private final GnPermissions gnPermissions;

    @Override
    public final GnPermissions getPermissions ()
    {
      return this.gnPermissions;
    }
      
    public BaseGnSecurityProfile (final long itsAid, final GnPermissions gnPermissions)
    {
      this.itsAid = itsAid;
      this.gnPermissions = gnPermissions;
    }
    
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // GN TRAFFIC CLASS
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  interface GnTrafficClass
  {
    
    byte toByte ();
    
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // DEFAULT GN TRAFFIC CLASS
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static class DefaultGnTrafficClass
  implements GnTrafficClass
  {
    
    private final byte trafficClassByte;
    
    public final byte getTrafficClassByte ()
    {
      return this.trafficClassByte;
    }
    
    @Override
    public final byte toByte ()
    {
      return this.trafficClassByte;
    }
    
    public DefaultGnTrafficClass (final byte trafficClassByte)
    {
      this.trafficClassByte = trafficClassByte;
//      XXX AUGMENT (Default)TrafficClass as shown below...
//      final int tcReservedBit = (((int) trafficClassByte) & 0x80) >> 7;
//      final int tcRelevanceBits = (((int) trafficClassByte) & 0x70) >> 6;
//      final int tcReliabilityBits = (((int) trafficClassByte) & 0x0c) >> 2;
//      final int tcLatencyBits = ((int) trafficClassByte) & 0x03;
//      System.err.println ("TC = " + (((int) trafficClassByte) & 0xff) + ".");
//      System.err.println ("  Reserved    = " + tcReservedBit + ".");
//      System.err.println ("  Relevance   = " + tcRelevanceBits + ".");
//      System.err.println ("  Reliability = " + tcReliabilityBits + ".");
//      System.err.println ("  Latency     = " + tcLatencyBits + ".");
    }
    
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // GN POSITION VECTOR
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  interface GnPositionVector
  {
    
    double getLatitude ();
    
    double getLongitude ();
    
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // DEFAULT GN POSITION VECTOR
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static class DefaultGnPositionVector
  implements GnPositionVector
  {
    
    // XXX Aren't there more fields in an LPV (speed/heading/etc.)??
    public DefaultGnPositionVector (final double latitude, final double longitude)
    {
      // XXX Error checking / range conversion (cardinal range?).
      this.latitude = latitude;
      this.longitude = longitude;
    }
    
    private final double latitude;

    @Override
    public final double getLatitude ()
    {
      return this.latitude;
    }
    
    private final double longitude;

    @Override
    public final double getLongitude ()
    {
      return this.longitude;
    }
    
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // GN SECURITY REPORT
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  interface GnSecurityReport
  {
    
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // GN CERTIFICATE ID
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  interface GnCertificateId
  {
    
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // GN PERMISSIONS
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /** The GN Permissions as referred to in the BTP SAP (ETSI EN 302636-5-1 V2.1.1 (2017-08).
   * 
   * <p>
   * GN Permissions are <i>always</i> interpreted in the sense of ETSI TS 103 097 V1.2.1 (2015-06),
   * i.e., an OCTET STRING with at most 31 elements.
   * 
   */
  interface GnPermissions
  {
    
    byte[] getPermissionsArray ();
    
  }
  
  /** A base implementation of {@link GnPermissions}.
   * 
   * <p>
   * Note that we refrain from using the {@code Default} prefix as it leads to confusion with
   * "default permissions" in the security context.
   * 
   * <p>
   * Objects of this class are immutable; this should be respected by sub-classes.
   * 
   */
  public static class BaseGnPermissions
  implements GnPermissions, Cloneable
  {
    
    private final byte[] permissionsArray;
    
    @Override
    public final synchronized byte[] getPermissionsArray ()
    {
      return Arrays.copyOf (this.permissionsArray, this.permissionsArray.length);
    }
    
    public BaseGnPermissions (final byte[] permissionsArray)
    {
      if (permissionsArray != null && permissionsArray.length > 31)
        throw new IllegalArgumentException ();
      this.permissionsArray = (permissionsArray != null) ? permissionsArray : new byte[]{};
    }

    public BaseGnPermissions ()
    {
      this (null);
    }

    @Override
    protected BaseGnPermissions clone () throws CloneNotSupportedException
    {
      return (BaseGnPermissions) super.clone ();
    }
    
  }
  
}
