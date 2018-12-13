/*
 * Copyright 2016-2018 Jan de Jongh, TNO.
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
package net.etsi.btpsap.client.udptno;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.etsi.btpsap.BtpSapTypes;
import net.etsi.btpsap.BtpSapTypes.BtpType;
import net.etsi.btpsap.BtpSap_DataReqContainer;

/** Formatting and parsing UDP-TNO BtpSap Request PDUs.
 * 
 * @author Jan de Jongh, TNO.
 * 
 * @see BtpSap_DataReqContainer
 * 
 */
public class UdpTnoDataReq
{

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // LOGGING
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG = Logger.getLogger (UdpTnoDataReq.class.getName ());
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // CONSTRUCTORS / FACTORIES / CLONING
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /** Inhibits instantiation.
   * 
   */
  private UdpTnoDataReq ()
  {
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // MAGIC BYTES
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private final static byte MAGIC_1 = (byte) (Integer.parseInt ("3d", 16) & 0xff);
  private final static byte MAGIC_2 = (byte) (Integer.parseInt ("93", 16) & 0xff);
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // ITS AIDs
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final long ITS_AID_CAM = 36;
	public static final long ITS_AID_DENM = 37;
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // FORMAT REQUEST
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /** Creates a BTP Data Request PDU from given parameters.
   * 
   * @param request  The BTP Data Request (non-{@code null}).
   * @param clientID The client ID (between 0 and 127 inclusive).
   * @param unitIds  The ID's of the units to which the request applies (this is encoded into the PDU).
   * 
   * @return The BTP Data Request PDU, or {@code null} in case of a failure.
   * 
   * @see #parseRequest
   * 
   */
  public final static byte [] formatRequest (final BtpSap_DataReqContainer request, final int clientID, final Set<Integer> unitIds)
  {
    if (request == null)
      return null;
    if (clientID < 0 || clientID > 127)
      return null;
    final int MAX_PDU_SIZE = 2048; // IMPLEMENTATION REQUIRES THIS TO BE MULTIPLE OF FOUR!
    final byte[] pdu = new byte[MAX_PDU_SIZE];
    int index = 0;
    // 0, 1: MAGIC
    pdu[index++] = MAGIC_1;
    pdu[index++] = MAGIC_2;
    // 2: version (1)
    pdu[index++] = (byte) 1;
    // 3: clientId
    pdu[index++] = (byte) clientID;
    // 4, 5: units
    byte unitsHigh = 0;
    byte unitsLow = 0;
    if (unitIds != null)
    {
      for (final int unitId : unitIds)
        if (unitId >= 1 && unitId <= 8)
          unitsLow |= (1 << (unitId - 1));
        else if (unitId >= 9 && unitId <= 16)
          unitsHigh |= (1 << (unitId - 9));
        else
          LOG.log (Level.WARNING, "Illegal unit number: {0}; ignored!", unitId);
    }
    pdu[index++] = unitsHigh;
    pdu[index++] = unitsLow;
    // 6: btpFlags
    final int commProfileBits;
    final BtpSapTypes.GnCommunicationsProfile gnCommunicationsProfile = request.getCommunicationsProfile ();
    if (gnCommunicationsProfile == null)
      commProfileBits = 0; // Silently default to ITS-G5...
    else
      switch (gnCommunicationsProfile)
      {
        case GN_COMPROF_ITSG5:
          commProfileBits = 0;
          break;
        case GN_COMPROF_CELLULAR:
          commProfileBits = 1;
          break;
        default:
          LOG.log (Level.WARNING, "Unknown communications profile {0}; request ignored!", gnCommunicationsProfile);
          return null;
      }
    final int btpTypeBits;
    final BtpType btpType = request.getBtpType (); // Non-null!
    switch (btpType)
    {
      case BTP_A:
        btpTypeBits = 0;
        break;
      case BTP_B:
        btpTypeBits = 1;
        break;
      default:
        LOG.log (Level.WARNING, "Unknown BTP Type {0}; request ignored!", btpType);
        return null;
    }
    pdu[index++] = (byte) ((commProfileBits << 4) + btpTypeBits);
    // 7: gnTransportType
    final int transportTypeNibble;
    final BtpSapTypes.GnTransportType gnTransportType = request.getGnTransportType (); // Non-null!
    switch (gnTransportType)
    {
      case GN_UC:
        transportTypeNibble = 0;
        break;
      case GN_SHB:
        transportTypeNibble = 1;
        break;
      case GN_TSB:
        transportTypeNibble = 2;
        break;
      case GN_GBC:
        transportTypeNibble = 3;
        break;
      case GN_AC:
        transportTypeNibble = 4;
        break;
      default:
        LOG.log (Level.WARNING, "Unknown GN Transport Type {0}; request ignored!", gnTransportType);
        return null;
    }
    final int gnSubTypeNibble;
    switch (gnTransportType)
    {
      case GN_UC:
      case GN_SHB:
      case GN_TSB:
        gnSubTypeNibble = 0;
        break;
      case GN_GBC:
      case GN_AC:
        final BtpSapTypes.GnAreaShape gnAreaShape = request.getGnDestination ().getGnArea ().getAreaShape ();
        switch (gnAreaShape)
        {
          case CIRCLE:
            gnSubTypeNibble = 0;
            break;
          case RECTANGLE:
            gnSubTypeNibble = 1;
            break;
          case ELLIPSE:
            gnSubTypeNibble = 2;
            break;
          default:
            LOG.log (Level.WARNING, "Unknown GN Area Shape {0}; request ignored!", gnAreaShape);
            return null;
        }
        break;
      default:
        throw new RuntimeException ();
    }
    pdu[index++] = (byte) ((transportTypeNibble << 4) + gnSubTypeNibble);
    // 8, 9: source port
    final int srcPort = request.getBtpSrcPort () != null ? request.getBtpSrcPort () : 0;
    pdu[index++] = (byte) ((srcPort & 0xff00) >> 8);
    pdu[index++] = (byte) (srcPort & 0x00ff);
    // 10: lifetime
    pdu[index++] = encodeMaxLifetime (request.getMaxLifeTime_ms ());
    // 11: Traffic Class
    pdu[index++] = request.getGnTrafficClass ().toByte ();
    // 12, 13: Destination Port
    final int dstPort = request.getBtpDestinationPort (); // Non-null.
    pdu[index++] = (byte) ((dstPort & 0xff00) >> 8);
    pdu[index++] = (byte) (dstPort & 0x00ff);
    // 14, 15: Destination Port Info [Optional]
    final Integer btpDstPortInfo = request.getBtpDstPortInfo ();
    pdu[index++] = (btpDstPortInfo != null ? (byte) ((btpDstPortInfo & 0xff00) >> 8) : (byte) 0);
    pdu[index++] = (btpDstPortInfo != null ? (byte) (btpDstPortInfo & 0x00ff) : (byte) 0);
    // 16: Hop Limit
    pdu[index++] = (byte) (request.getGnMaxHopLimit () & 0xff);
    // 17: Repetition Interval -> Unsupported by BtpSap (XXX).
    pdu[index++] = (byte) 0; // Means disable completely.
    // 18: Repitition Time -> Unsupported by BtpSap (XXX).
    pdu[index++] = (byte) 0;
    // 19: RESERVED_1 -> Set to 0x00.
    pdu[index++] = (byte) 0;
    // 20 - 27: LATITUDE [NOT GN_UC] or GN_UC_DST_ADDRESS [GN_UC]
    final BtpSapTypes.GnDestination gnDestination = request.getGnDestination (); // Non-null.
    switch (gnTransportType)
    {
      case GN_SHB:
      case GN_TSB:
        for (int i = 1; i <= 8; i++)
          pdu[index++] = (byte) 0;
        break;
      case GN_UC:
        final long gnUnicastAddress = gnDestination.getGnUnicastAddress ().toLong ();
        pdu[index++] = (byte) ((gnUnicastAddress & 0xff00000000000000L) >> 56);
        pdu[index++] = (byte) ((gnUnicastAddress & 0x00ff000000000000L) >> 48);
        pdu[index++] = (byte) ((gnUnicastAddress & 0x0000ff0000000000L) >> 40);
        pdu[index++] = (byte) ((gnUnicastAddress & 0x000000ff00000000L) >> 32);
        pdu[index++] = (byte) ((gnUnicastAddress & 0x00000000ff000000L) >> 24);
        pdu[index++] = (byte) ((gnUnicastAddress & 0x0000000000ff0000L) >> 16);
        pdu[index++] = (byte) ((gnUnicastAddress & 0x000000000000ff00L) >>  8);
        pdu[index++] = (byte) ((gnUnicastAddress & 0x00000000000000ffL));
        break;        
      case GN_GBC:
      case GN_AC:
        final double latitude = gnDestination.getGnArea ().getLatitude ();
        final long latIEEE754Bits = Double.doubleToLongBits (latitude);
        pdu[index++] = (byte) ((latIEEE754Bits & 0xff00000000000000L) >> 56);
        pdu[index++] = (byte) ((latIEEE754Bits & 0x00ff000000000000L) >> 48);
        pdu[index++] = (byte) ((latIEEE754Bits & 0x0000ff0000000000L) >> 40);
        pdu[index++] = (byte) ((latIEEE754Bits & 0x000000ff00000000L) >> 32);
        pdu[index++] = (byte) ((latIEEE754Bits & 0x00000000ff000000L) >> 24);
        pdu[index++] = (byte) ((latIEEE754Bits & 0x0000000000ff0000L) >> 16);
        pdu[index++] = (byte) ((latIEEE754Bits & 0x000000000000ff00L) >>  8);
        pdu[index++] = (byte) ((latIEEE754Bits & 0x00000000000000ffL));
        break;        
      default:
        throw new RuntimeException ();
    }
    // 28 - 35: LONGITUDE
    switch (gnTransportType)
    {
      case GN_UC:
      case GN_SHB:
      case GN_TSB:
        for (int i = 1; i <= 8; i++)
          pdu[index++] = (byte) 0;
        break;
      case GN_GBC:
      case GN_AC:
        final double longitude = gnDestination.getGnArea ().getLongitude ();
        final long lonIEEE754Bits = Double.doubleToLongBits (longitude);
        pdu[index++] = (byte) ((lonIEEE754Bits & 0xff00000000000000L) >> 56);
        pdu[index++] = (byte) ((lonIEEE754Bits & 0x00ff000000000000L) >> 48);
        pdu[index++] = (byte) ((lonIEEE754Bits & 0x0000ff0000000000L) >> 40);
        pdu[index++] = (byte) ((lonIEEE754Bits & 0x000000ff00000000L) >> 32);
        pdu[index++] = (byte) ((lonIEEE754Bits & 0x00000000ff000000L) >> 24);
        pdu[index++] = (byte) ((lonIEEE754Bits & 0x0000000000ff0000L) >> 16);
        pdu[index++] = (byte) ((lonIEEE754Bits & 0x000000000000ff00L) >>  8);
        pdu[index++] = (byte) ((lonIEEE754Bits & 0x00000000000000ffL));
        break;
      default:
        throw new RuntimeException ();        
    }
    // 36, 37: DISTANCE_A
    // 38, 39: DISTANCE_B
    // 40, 41: ANGLE
    switch (gnTransportType)
    {
      case GN_UC:
      case GN_SHB:
      case GN_TSB:
        for (int i = 1; i <= 6; i++)
          pdu[index++] = (byte) 0;
        break;
      case GN_GBC:
      case GN_AC:
        // 3x uint16 in disguise...
        final int distanceA = gnDestination.getGnArea ().getDistanceA_m () & 0xffff;
        final int distanceB = gnDestination.getGnArea ().getDistanceA_m () & 0xffff;
        final int angle = gnDestination.getGnArea ().getAngle_degrees () & 0xffff;
        pdu[index++] = (byte) ((distanceA & 0xff00) >> 8);
        pdu[index++] = (byte) ((distanceA & 0x00ff));
        pdu[index++] = (byte) ((distanceB & 0xff00) >> 8);
        pdu[index++] = (byte) ((distanceB & 0x00ff));
        pdu[index++] = (byte) ((angle     & 0xff00) >> 8);
        pdu[index++] = (byte) ((angle     & 0x00ff));
        break;
      default:
        throw new RuntimeException ();        
    }
    // 42, 43: RESERVED_2
    pdu[index++] = (byte) 0;
    pdu[index++] = (byte) 0;
    // 44 - 55: SECURITY PROFILE
    // 44 - 47: ITS-AID
    // 48 - 55: SSP BITS
    final BtpSapTypes.GnSecurityProfile gnSecurityProfile = request.getSecurityProfile ();
    if (gnSecurityProfile != null)
    {
      // uint32 in disguise...
      final long itsAid = gnSecurityProfile.getItsAid () & 0xffffffffL;
      pdu[index++] = (byte) ((itsAid & 0xff000000L) >> 24);
      pdu[index++] = (byte) ((itsAid & 0x00ff0000L) >> 16);
      pdu[index++] = (byte) ((itsAid & 0x0000ff00L) >>  8);
      pdu[index++] = (byte) ((itsAid & 0x000000ffL));
      final BtpSapTypes.GnPermissions gnPermissions = gnSecurityProfile.getPermissions ();
      if (gnPermissions != null && gnPermissions.getPermissionsArray () != null)
      {
        final byte[] reqSspBytes = gnPermissions.getPermissionsArray ();
        if (reqSspBytes.length > 8)
        {
          LOG.log (Level.WARNING, "SSP byte array too large (up to 8 supported): {0}; request ignored!",
            reqSspBytes.length);
          return null;
        }
        final byte[] pduSspBytes = new byte[8]; // Initialized to zero-byte values!
        if (reqSspBytes.length > 0)
          System.arraycopy (reqSspBytes, 0, pduSspBytes, 8 - reqSspBytes.length, reqSspBytes.length);
        int j = 0;
        for (int i = 1; i <= 8; i++)
          pdu[index++] = pduSspBytes[j++];
      }
      else
        for (int i = 1; i <= 8; i++)
          pdu[index++] = (byte) 0;
    }
    else
    {
      for (int i = 1; i <= 12; i++)
        pdu[index++] = (byte) 0;
    }
    // 56 - 59: PAYLOAD LENGTH
    final int reqPayloadLength = request.getLength ();
    if (reqPayloadLength < 0)
    {
      LOG.log (Level.WARNING, "Invalid payload size: {0}; request ignored!", reqPayloadLength);
      return null;      
    }
    final int reqPayloadOffset = request.getOffset ();
    if (reqPayloadOffset < 0)
    {
      LOG.log (Level.WARNING, "Invalid payload offset: {0}; request ignored!", reqPayloadOffset);
      return null;      
    }
    final byte[] reqPayload = request.getData ();
    if (reqPayload == null && reqPayloadLength > 0)
    {
      LOG.log (Level.WARNING, "Invalid payload length: {0} for null-provided payload data; request ignored!", reqPayloadLength);
      return null;            
    }
    if (reqPayload != null && reqPayloadOffset + reqPayloadLength > reqPayload.length)
    {
      LOG.log (Level.WARNING, "Invalid payload specification: offset {0} + length {1} > payloadLength {2}; request ignored!",
        new Object[]{reqPayloadOffset, reqPayloadLength, reqPayload.length});
      return null;
    }
    pdu[index++] = (byte) ((reqPayloadLength & 0x7f000000) >> 24);
    pdu[index++] = (byte) ((reqPayloadLength & 0x00ff0000) >> 16);
    pdu[index++] = (byte) ((reqPayloadLength & 0x0000ff00) >> 8);
    pdu[index++] = (byte) ((reqPayloadLength & 0x000000ff));
    // 60 - (60 + PayloadLength - 1): PAYLOAD [if PayloadLength > 0].
    if (reqPayload != null)
    {
      if (index + reqPayloadLength > MAX_PDU_SIZE)
      {
        LOG.log (Level.WARNING, "Payload size too large: {1} > {2} (maximum); request ignored!",
          new Object[]{reqPayloadLength, MAX_PDU_SIZE - index});
        return null;
      }
      System.arraycopy (reqPayload, reqPayloadOffset, pdu, index, reqPayloadLength);
      index += reqPayloadLength;
    }
    // 60 + PayloadLength 32-BIT BOUNDARY PADDING
    while (index % 4 != 0)
      pdu[index++] = (byte) 0;
    // Trim the PDU; at this point 'index' holds the actual (trimmed) PDU size.
    final int trimmedPduLength = index;
    final byte[] trimmedPdu = new byte[trimmedPduLength];
    System.arraycopy (pdu, 0, trimmedPdu, 0, trimmedPduLength);
    return trimmedPdu;
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // PARSE REQUEST
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public final static BtpSap_DataReqContainer parseRequest
  ( final byte[] pdu,
    final int offset,
    final int length,
    AtomicInteger clientIdContainer,
    final Set<Integer> unitIdContainer)
  {
    if (pdu == null)
    {
      LOG.log (Level.SEVERE, "Received null PDU!");
      return null;
    }
    if (offset < 0 || length < 0 || offset + length > pdu.length)
    {
      LOG.log (Level.WARNING, "Received UDP packet with invalid size and/or offset!");
      return null;
    }
    if (length < 60)
    {
      LOG.log (Level.WARNING, "Received UDP packet with invalid size!");
      return null;
    }
    // Check magic: 0x3d93.
    if (pdu[offset] != MAGIC_1 || pdu[offset + 1] != MAGIC_2)
    {
      LOG.log (Level.WARNING, "Received UDP packet with magic mismatch (!= 0x3d93)!");
      return null;
    }
    // Check version: only 0x01 supported.
    if (pdu[offset + 2] != 1)
    {
      LOG.log (Level.WARNING, "Received UDP packet with unsupported version number: {0}.", pdu[offset + 2]);
      return null;
    }
    final int clientId = pdu[offset + 3] & 0xff;
    if (clientIdContainer != null)
      clientIdContainer.set (clientId);
    LOG.log (Level.FINE, "Received Data Request from client ID {0}.", clientId);
    // Extract set of units (servers) to which the request applies.
    final int unitsHigh = pdu[offset + 4] & 0xff;
    final int unitsLow  = pdu[offset + 5] & 0xff;
    createUnitIdContainer (unitsHigh, unitsLow, unitIdContainer);
    boolean error = false;
    final List<String> errorMessages = new ArrayList<> ();
    final byte btpFlags = pdu[offset + 6];
    final int communicationsProfileBits = (((int) btpFlags) & 0xff) >> 4;
    final BtpSapTypes.GnCommunicationsProfile gnCommunicationsProfile;
    switch (communicationsProfileBits)
    {
      case 0:
        gnCommunicationsProfile = BtpSapTypes.GnCommunicationsProfile.GN_COMPROF_ITSG5;
        break;
      case 1:
        gnCommunicationsProfile = BtpSapTypes.GnCommunicationsProfile.GN_COMPROF_CELLULAR;
        break;
      default:
        gnCommunicationsProfile = null;
        error = true;
        errorMessages.add ("Unknown Communications Profile: " + communicationsProfileBits + ".");
        break;
    }
    final int btpTypeBits = (((int) btpFlags) & 0x0f);
    final BtpSapTypes.BtpType btpType;
    switch (btpTypeBits)
    {
      case 0:
        btpType = BtpSapTypes.BtpType.BTP_A;
        break;
      case 1:
        btpType = BtpSapTypes.BtpType.BTP_B;
        break;
      default:
        btpType = null;
        error = true;
        errorMessages.add ("Unknown BTP Type: " + btpTypeBits + ".");
        break;
    }
    final byte gnTypeByte = pdu[offset + 7];
    final int gnTypeNibble = (((int) gnTypeByte) & 0xff) >> 4;
    final BtpSapTypes.GnTransportType gnTransportType;
    switch (gnTypeNibble)
    {
      case 0:
        gnTransportType = BtpSapTypes.GnTransportType.GN_UC;
        break;
      case 1:
        gnTransportType = BtpSapTypes.GnTransportType.GN_SHB;
        break;
      case 2:
        gnTransportType = BtpSapTypes.GnTransportType.GN_TSB;
        break;
      case 3:
        gnTransportType = BtpSapTypes.GnTransportType.GN_GBC;
        break;
      case 4:
        gnTransportType = BtpSapTypes.GnTransportType.GN_AC;
        break;
      default:
        gnTransportType = null;
        error = true;
        errorMessages.add ("Unknown GeoNetworking Transport Type: " + gnTypeNibble + ".");
        break;
    }
    final int sourcePort = ((pdu[offset + 8] & 0xff) << 8) + (pdu[offset + 9] & 0xff);
    final byte lifeTimeByte = pdu[offset + 10];
    final int lifeTimeMultiplier = (((int) lifeTimeByte) & 0xff) >> 2;
    final int lifeTimeBaseBits = ((int) lifeTimeByte) & 0x03;
    final int lifeTimeBase_ms;
    switch (lifeTimeBaseBits)
    {
      case 0:
        lifeTimeBase_ms = 50;
        break;
      case 1:
        lifeTimeBase_ms = 1000;
        break;
      case 2:
        lifeTimeBase_ms = 10000;
        break;
      case 3:
        lifeTimeBase_ms = 100000;
        break;
      default:
        lifeTimeBase_ms = 0;
        error = true;
        errorMessages.add ("Unknown Lifetime Base: " + lifeTimeBaseBits + ".");
        break;
    }
    final int lifetime_ms = lifeTimeMultiplier * lifeTimeBase_ms;
    final byte tcByte = pdu[offset + 11];
    final BtpSapTypes.GnTrafficClass gnTrafficClass = new BtpSapTypes.DefaultGnTrafficClass (tcByte);
    final int destinationPort = ((pdu[offset + 12] & 0xff) << 8) + (pdu[offset + 13] & 0xff);
    final int destinationPortInfo = ((pdu[offset + 14] & 0xff) << 8) + (pdu[offset + 15] & 0xff);
    final int hopLimit = pdu[offset + 16] & 0xff;
    final byte repIntervalByte = pdu[offset + 17];
    final int repIntervalMultiplier = (((int) repIntervalByte) & 0xff) >> 2;
    final int repIntervalBaseBits = ((int) repIntervalByte) & 0x03;
    final int repIntervalBase_ms;
    switch (repIntervalBaseBits)
    {
      case 0:
        repIntervalBase_ms = 50;
        break;
      case 1:
        repIntervalBase_ms = 1000;
        break;
      case 2:
        repIntervalBase_ms = 10000;
        break;
      case 3:
        repIntervalBase_ms = 100000;
        break;
      default:
        repIntervalBase_ms = 0;
        error = true;
        errorMessages.add ("Unknown Repitition Interval Base: " + repIntervalBaseBits + ".");
        break;
    }
    final int repInterval_ms = repIntervalMultiplier * repIntervalBase_ms;
    final byte repTimeByte = pdu[offset + 18];
    final int repTimeMultiplier = (((int) repTimeByte) & 0xff) >> 2;
    final int repTimeBaseBits = ((int) repTimeByte) & 0x03;
    final int repTimeBase_ms;
    switch (repTimeBaseBits)
    {
      case 0:
        repTimeBase_ms = 50;
        break;
      case 1:
        repTimeBase_ms = 1000;
        break;
      case 2:
        repTimeBase_ms = 10000;
        break;
      case 3:
        repTimeBase_ms = 100000;
        break;
      default:
        repTimeBase_ms = 0;
        error = true;
        errorMessages.add ("Unknown Repitition Time Base: " + repTimeBaseBits + ".");
        break;
    }
    final int repTime_ms = repTimeMultiplier * repTimeBase_ms;
    // final byte reserved1Byte = pdu[offset + 19];
    // System.err.println ("RESERVED_1: " + (((int) reserved1Byte) & 0xff) + ".");
    final BtpSapTypes.GnDestination gnDestination;
    if (gnTransportType == null)
      gnDestination = null;
    else
      switch (gnTransportType)
      {
        case GN_UC:
          final byte[] gnAddressBytes = Arrays.copyOfRange (pdu, 20, 28);
          // XXX Error checking on gnAddressBytes??
          final BtpSapTypes.GnAddress gnAddress = new BtpSapTypes.DefaultGnAddress (gnAddressBytes);
          gnDestination = new BtpSapTypes.DefaultGnDestination (gnAddress);
          break;
        case GN_SHB:
          gnDestination = null;
          break;
        case GN_TSB:
          gnDestination = null;
          break;
        case GN_GBC:
        case GN_AC:
          final int gnSubTypeNibble = (((int) gnTypeByte) & 0x0f);
          final BtpSapTypes.GnAreaShape gnAreaShape;
          switch (gnSubTypeNibble)
          {
            case 0:
              gnAreaShape = BtpSapTypes.GnAreaShape.CIRCLE;
              break;
            case 1:
              gnAreaShape = BtpSapTypes.GnAreaShape.RECTANGLE;
              break;
            case 2:
              gnAreaShape = BtpSapTypes.GnAreaShape.ELLIPSE;
              break;
            default:
              gnAreaShape = null;
              error = true;
              errorMessages.add ("Unknown GeoNetworking SubType: " + gnSubTypeNibble + ".");
              break;
          }
          final long latBits = ((long) (pdu[offset + 20] & 0xff) << 56)
                             + ((long) (pdu[offset + 21] & 0xff) << 48)
                             + ((long) (pdu[offset + 22] & 0xff) << 40)
                             + ((long) (pdu[offset + 23] & 0xff) << 32)
                             + ((long) (pdu[offset + 24] & 0xff) << 24)
                             + ((long) (pdu[offset + 25] & 0xff) << 16)
                             + ((long) (pdu[offset + 26] & 0xff) << 8)
                             + ((long) (pdu[offset + 27] & 0xff));
          final double latitude = Double.longBitsToDouble (latBits);
          final long lonBits = ((long) (pdu[offset + 28] & 0xff) << 56)
                             + ((long) (pdu[offset + 29] & 0xff) << 48)
                             + ((long) (pdu[offset + 30] & 0xff) << 40)
                             + ((long) (pdu[offset + 31] & 0xff) << 32)
                             + ((long) (pdu[offset + 32] & 0xff) << 24)
                             + ((long) (pdu[offset + 33] & 0xff) << 16)
                             + ((long) (pdu[offset + 34] & 0xff) << 8)
                             + ((long) (pdu[offset + 35] & 0xff));
          final double longitude = Double.longBitsToDouble (lonBits);
          final int dA = ((pdu[offset + 36] & 0xff) << 8) + (pdu[offset + 37] & 0xff);
          final int dB = ((pdu[offset + 38] & 0xff) << 8) + (pdu[offset + 39] & 0xff);
          final int angle = ((pdu[offset + 40] & 0xff) << 8) + (pdu[offset + 41] & 0xff);
          if (error)
            gnDestination = null;
          else
            gnDestination = new BtpSapTypes.DefaultGnDestination (new BtpSapTypes.DefaultGnArea (gnAreaShape, latitude, longitude, dA, dB, angle));
          break;
        default:
          gnDestination = null;
          break;
      }
    // final int reserved2Int = ((pdu[offset + 42] & 0xff) << 8) + (pdu[offset + 43] & 0xff);
    // System.err.println ("RESERVED_2: " + reserved2Int + ".");
    //
    // Next 12 octets starting at offset 44 are reserved for the security profile.
    // We interpret the security profile as consisting of an ITS-AID and Service-Specific Permissions.
    // For now, we reserve the first 4 octets for the ITS-AID encoded as an (unsigned) int.
    // In Java, we simply put it into a (signed) long, and promiss to be careful with negative values.
    // The remaining 8 octets are reserved for future use in the interface, in particular,
    // for passing ssp (service-specific security permissions).
    // For now, we set the ssp value to null in the constructor of the security profile.
    //
    final long itsAidRead = ByteBuffer.wrap (pdu, 44, 4).getInt () & 0xffffffffL; // XXX Do this properly!!
    final long itsAid;
    if (itsAidRead == 0L && gnCommunicationsProfile == BtpSapTypes.GnCommunicationsProfile.GN_COMPROF_ITSG5)
    {
      // Courtesy substitution of ITS-AID for CAM and DENM.
      // Well-known BTP port numbers: ETSI EN 302 636-5-1 V1.2.0 (2013-10); Annex B.
      // XXX: Add TOPO/SPAT/SAM?
      // XXX Should be split off into separate (static) method.
      if (btpType == BtpSapTypes.BtpType.BTP_B && gnTransportType == BtpSapTypes.GnTransportType.GN_SHB
        && destinationPort == 2001)
      {
        itsAid = ITS_AID_CAM;
        LOG.log (Level.FINE, "Courtesy ITS-AID substitution for (apparent) CAM request.");
      }
      else if (btpType == BtpSapTypes.BtpType.BTP_B && gnTransportType == BtpSapTypes.GnTransportType.GN_GBC
        && destinationPort == 2002)
      {
        itsAid = ITS_AID_DENM;
        LOG.log (Level.FINE, "Courtesy ITS-AID substitution for (apparent) DENM request.");
      }
      else
      {
        itsAid = -1; // ITS_AID_ANY
        LOG.log (Level.FINE, "Courtesy ITS-AID (ANY) substitution.");
      }
    }
    else
      itsAid = itsAidRead;
    final byte[] itsSspRead = new byte[8];
    ByteBuffer.wrap (pdu, 48, 8).get (itsSspRead);
    final BtpSapTypes.GnPermissions permissions = new BtpSapTypes.BaseGnPermissions (itsSspRead);
    final BtpSapTypes.GnSecurityProfile gnSecurityProfile = new BtpSapTypes.BaseGnSecurityProfile (itsAid, permissions);
    final long payloadLength = ((long) (pdu[offset + 56] & 0xff) << 24)
                             + ((long) (pdu[offset + 57] & 0xff) << 16)
                             + ((long) (pdu[offset + 58] & 0xff) << 8)
                             + ((long) (pdu[offset + 59] & 0xff));
    long payloadAndPaddingLength = payloadLength;
    while (payloadAndPaddingLength % 4 != 0)
      payloadAndPaddingLength++;
    if (length != 60 + payloadAndPaddingLength)
    {
      error = true;
      errorMessages.add ("UDP Packet Length and Payload+Padding Length MISMATCH: UDP packet length = " + length
        + ", payload length encoded in packet = " + payloadLength
        + ", calculated payload+padding length = " + payloadAndPaddingLength
        + " [SHOULD BE EXACTLY 60 LESS THAN UDP PACKET SIZE]!");
    }
    // System.err.println ("Payload:");
    // System.err.println (Arrays.toString (Arrays.copyOfRange (pdu, (int) 60, (int) (60 + payloadLength))));
    if (error)
    {
      LOG.log (Level.WARNING, "Error decoding BTP/UDP[TNO] packet from client ID {0}:", clientId);
      for (final String errorMessage : errorMessages)
        LOG.log (Level.WARNING, errorMessage);
      return null;
    }
    else
    {
      final BtpSap_DataReqContainer reqContainer;
      try
      {
        reqContainer = new BtpSap_DataReqContainer
          (btpType, sourcePort, destinationPort, destinationPortInfo,
           gnTransportType, gnDestination, gnCommunicationsProfile, gnSecurityProfile,
           lifetime_ms, repInterval_ms, repTime_ms, hopLimit, gnTrafficClass,
           (int) payloadLength, 60, pdu);
        return reqContainer;
      }
      catch (IllegalArgumentException iae)
      {
        LOG.log (Level.WARNING, "Error creating BTP Request Container: "
          + "Illegal Argument (dropping request from client ID {0}): {1}.",
          new Object[] {clientId, iae.getMessage ()});
        return null;
      }
    }
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // ENCODE LIFETIME
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
 
  private static byte encodeMaxLifetime (final int maxLifetime_ms)
  {
    if (maxLifetime_ms <= 0)
      return (byte) 0;
    if (maxLifetime_ms <= 50 * 63)
      return (byte) (((int) Math.round (maxLifetime_ms / 50.0)) << 2);
    if (maxLifetime_ms <= 1000 * 63)
      return (byte) (1 + (((int) Math.round (maxLifetime_ms / 1000.0)) << 2));
    if (maxLifetime_ms <= 10000 * 63)
      return (byte) (2 + (((int) Math.round (maxLifetime_ms / 10000.0)) << 2));
    if (maxLifetime_ms <= 100000 * 63)
      return (byte) (3 + (((int) Math.round (maxLifetime_ms / 100000.0)) << 2));
    return (byte) 0xff;
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // CREATE UNITS SET
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
 
  private static void createUnitIdContainer (final int unitsHigh, final int unitsLow, final Set<Integer> unitIdContainer)
  {
    if (unitIdContainer == null)
      return;
    unitIdContainer.clear ();
    if ((unitsLow & 0x01) != 0)
      unitIdContainer.add (1);
    if ((unitsLow & 0x02) != 0)
      unitIdContainer.add (2);
    if ((unitsLow & 0x04) != 0)
      unitIdContainer.add (3);
    if ((unitsLow & 0x08) != 0)
      unitIdContainer.add (4);
    if ((unitsLow & 0x10) != 0)
      unitIdContainer.add (5);
    if ((unitsLow & 0x20) != 0)
      unitIdContainer.add (6);
    if ((unitsLow & 0x40) != 0)
      unitIdContainer.add (7);
    if ((unitsLow & 0x80) != 0)
      unitIdContainer.add (8);
    if ((unitsHigh & 0x01) != 0)
      unitIdContainer.add (9);
    if ((unitsHigh & 0x02) != 0)
      unitIdContainer.add (10);
    if ((unitsHigh & 0x04) != 0)
      unitIdContainer.add (11);
    if ((unitsHigh & 0x08) != 0)
      unitIdContainer.add (12);
    if ((unitsHigh & 0x10) != 0)
      unitIdContainer.add (13);
    if ((unitsHigh & 0x20) != 0)
      unitIdContainer.add (14);
    if ((unitsHigh & 0x40) != 0)
      unitIdContainer.add (15);
    if ((unitsHigh & 0x80) != 0)
      unitIdContainer.add (16);
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // END OF FILE
  //
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
 
}
