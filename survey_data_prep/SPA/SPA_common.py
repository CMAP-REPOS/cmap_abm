from collections import defaultdict
import numpy as np
import math
from SPA_proj_const import *


################## Dictionaries Defining Coding of Variables ##########################################
INDEX_JTRIP_BY_DEPTIME = 0  #1 if index by trip departure time at origin, 0 if by trip arrival time at destination

# # BMP [09/06/17] - updated to represent agg modes in SANDAG survey
# NewTripMode = {
#     'SOV-FREE': 1,
#     'SOV-PAY':  2,
#     'HOV2-FREE':  3,
#     'HOV2-PAY': 4,
#     'HOV3-FREE': 5,
#     'HOV3-PAY': 6,
#     'WALK': 7,
#     'BIKE': 8,
#     'TAXI': 9,
#     'SCHOOLBUS': 10,
#     'TRANSIT': 11,
#     'OTHER': 12
#     }

# DH [02/03/20] - added to represent agg modes in SEMCOG Activity-Based model design doc
NewTripMode = {
        'SOV': 1,
        'HOV2': 2,
        'HOV3': 3,
        'WALK': 4,
        'BIKE': 5,
        'WALK-TRANSIT': 6,
        'KNR-TRANSIT': 7,
        'PNR-TRANSIT': 8,
        'TNC-TRANSIT': 9,
        'TAXI': 10,
        'TNC-REG': 11,
        'TNC-POOL': 12,
        'SCHOOLBUS': 13,
        'TRANSIT': 100,
        'OTHER': 14
}

NewTransitAccess = {
    'PNR':  1,
    'KNR':  2,
    'WALK': 3,
    'TNC': 4
    }


NewTourMode = {
    'SOV': 1,
    'HOV2': 2,
    'HOV3': 3,
    'WALK': 4,
    'BIKE': 5,
    'WALK-TRANSIT': 6,
    'KNR-TRANSIT': 7,
    'PNR-TRANSIT': 8,
    'TNC-TRANSIT': 9,
    'TAXI': 10,
    'TNC-REG': 11,
    'TNC-POOL': 12,
    'SCHOOLBUS': 13,
    'TRANSIT': 100,
    'OTHER': 14
}


NewPurp = {     #map new purpose name to purpose code
    'HOME':         0,
    'WORK':         1,
    'UNIVERSITY':   2,
    'SCHOOL':       3,
    'ESCORTING':    4,
    'SHOPPING':     5,
    'MAINTENANCE':  6,
    'EAT OUT':      7,
    'SOCIAL/VISIT': 8,
    'DISCRETIONARY':9,
    'WORK-RELATED': 10,
    'LOOP':         11,
    'CHANGE MODE':  12,
    'OTHER':        13
    }

NewPartialTour = {  #map partial tour types to numeric code
    'NOT_PARTIAL':      0,  #not a partial tour
    'PARTIAL_START':    1,  #first tour of the day not starting at home
    'PARTIAL_END':      2   #last tour of the day not ending at home
    }

NewEscort = {
    'NEITHER':  0,  #neither pick up or drop off
    'PICK_UP':  1,  #pick up passenger
    'DROP_OFF': 2,   #drop off passenger
    'PICK_UP_NON_HH':     3,   #pick up non household member
    'DROP_OFF_NON_HH':    4,   #pick up non household member
    'BOTH_PUDO':          5
    }

NewEscortType = {
        'RIDE_SHARING': 1,
        'PURE_ESCORT': 2,
        'NO_ESCORT': 3
        }

# DH [02/03/20] - updated to match SEMCOG model design doc
# LF updated for MWCOG
NewPerType = {
    'FW' : 1,
    'PW' : 2,
    'US' : 3,
    'NW' : 4,
    'RE' : 5,
    'DS' : 6,
    'ND' : 7,
    'PS' : 8
    }

NewStuCategory = {
    'SCHOOL':       1,
    'UNIVERSITY':   2,
    'NON-STUDENT':  3
    }

NewEmpCategory = {
    'FULLTIME':     1,
    'PARTTIME':     2,
    'UNEMPLOYED':   3,
    'NON-WORKER':   4
    }

NewCompType = {  #map travel party composition label to code
    'ALL_ADULTS':       1,
    'ALL_CHILDREN':     2,
    'MIXED':            3
    }

NewJointCategory = {
    'NOT-JOINT':    0,
    'JOINT':        1,
    'JOINT-GROUPED':  2
    }

NewJointTourStatus = {
'INDEPENDENT':          1,
'PART_JOINT':           2,
'FULL_JOINT':           3,
'PROB_JOINT':           4
}

################## Dictionaries Defining Fields in Output files ##########################################


TripCol2Name = {    #map trip table column number to column header
    1: 'HH_ID',
    2: 'PER_ID',
    3: 'TOUR_ID',
    4: 'TRIP_ID',
    5: 'ORIG_PLACENO',
    6: 'ORIG_X',
    7: 'ORIG_Y',
    8: 'ORIG_TAZ',
    9: 'ORIG_MAZ',
    10: 'DEST_PLACENO',
    11: 'DEST_X',
    12: 'DEST_Y',
    13: 'DEST_TAZ',
    14: 'DEST_MAZ',
    15: 'ORIG_PURP',
    16: 'DEST_PURP',
    17: 'ORIG_ARR_HR',
    18: 'ORIG_ARR_MIN',
    19: 'ORIG_ARR_BIN',
    20: 'ORIG_DEP_HR',
    21: 'ORIG_DEP_MIN',
    22: 'ORIG_DEP_BIN',
    23: 'DEST_ARR_HR',
    24: 'DEST_ARR_MIN',
    25: 'DEST_ARR_BIN',
    26: 'DEST_DEP_HR',
    27: 'DEST_DEP_MIN',
    28: 'DEST_DEP_BIN',
    29: 'TRIP_DUR_HR',
    30: 'TRIP_DUR_MIN',
    31: 'TRIP_DUR_BIN',
    32: 'TRIPMODE',
    33: 'ISDRIVER',
    34: 'CHAUFFUER_ID',
    35: 'AUTO_OCC',
    36: 'TOURMODE',
    37: 'TOURPURP',
    38: 'BOARDING_PLACENO',
    39: 'BOARDING_PNAME',
    40: 'BOARDING_X',
    41: 'BOARDING_Y',
    42: 'BOARDING_TAP',
    43: 'ALIGHTING_PLACENO',
    44: 'ALIGHTING_PNAME',
    45: 'ALIGHTING_X',
    46: 'ALIGHTING_Y',
    47: 'ALIGHTING_TAP',
    48: 'TRANSIT_NUM_XFERS',
    49: 'TRANSIT_ROUTE_1',
    50: 'TRANSIT_MODE_1',
    51: 'XFER_1_PLACENO',
    52: 'XFER_1_PNAME',
    53: 'XFER_1_X',
    54: 'XFER_1_Y',
    55: 'XFER_1_TAP',
    56: 'TRANSIT_ROUTE_2',
    57: 'TRANSIT_MODE_2',
    58: 'XFER_2_PLACENO',
    59: 'XFER_2_PNAME',
    60: 'XFER_2_X',
    61: 'XFER_2_Y',
    62: 'XFER_2_TAP',
    63: 'TRANSIT_ROUTE_3',
    64: 'TRANSIT_MODE_3',
    65: 'XFER_3_PLACENO',
    66: 'XFER_3_PNAME',
    67: 'XFER_3_X',
    68: 'XFER_3_Y',
    69: 'XFER_3_TAP',
    70: 'PARKING_PLACENO',
    71: 'PARKING_PNAME',
    72: 'PARKING_X',
    73: 'PARKING_Y',
    74: 'SUBTOUR',
    75: 'IS_INBOUND',
    76: 'TRIPS_ON_JOURNEY',
    77: 'TRIPS_ON_TOUR',
    78: 'ORIG_IS_TOUR_ORIG',
    79: 'ORIG_IS_TOUR_DEST',
    80: 'DEST_IS_TOUR_DEST',
    81: 'DEST_IS_TOUR_ORIG',
    82: 'PEREXPFACT',
    83: 'HHEXPFACT',
    84: 'PERSONTYPE',
    85: 'FULLY_JOINT',
    86: 'PARTIAL_TOUR',
    87: 'JTRIP_ID',
    88: 'ESCORTED',
    89: 'ESCORTING',
    90: 'NUM_PERSONS_ESCORTED',
    91: 'ESCORT_PERS_1',
    92: 'ESCORT_PERS_2',
    93: 'ESCORT_PERS_3',
    94: 'ESCORT_PERS_4',
    95: 'ESCORT_PERS_5',
    96: 'DEST_ESCORTING',
    97: 'JOINT',
    98: 'NUM_UL_JTRIPS',
    99: 'DIST',
    100: 'ERROR'
    }

#only fields listed below get written out to the tour file
TourCol2Name = {    #map tour table column number to column header
    1: 'HH_ID',
    2: 'PER_ID',
    3: 'TOUR_ID',
    4: 'ORIG_PLACENO',
    5: 'DEST_PLACENO',
    6: 'ORIG_X',
    7: 'ORIG_Y',
    8: 'ORIG_TAZ',
    9: 'ORIG_MAZ',
    10: 'DEST_X',
    11: 'DEST_Y',
    12: 'DEST_TAZ',
    13: 'DEST_MAZ',
    14: 'DEST_MODE',
    15: 'ORIG_MODE',
    16: 'TOURPURP',
    17: 'TOURMODE',
    18: 'DRIVER',
    19: 'ANCHOR_DEPART_HOUR',
    20: 'ANCHOR_DEPART_MIN',
    21: 'ANCHOR_DEPART_BIN',
    22: 'PRIMDEST_ARRIVE_HOUR',
    23: 'PRIMDEST_ARRIVE_MIN',
    24: 'PRIMDEST_ARRIVE_BIN',
    25: 'PRIMDEST_DEPART_HOUR',
    26: 'PRIMDEST_DEPART_MIN',
    27: 'PRIMDEST_DEPART_BIN',
    28: 'ANCHOR_ARRIVE_HOUR',
    29: 'ANCHOR_ARRIVE_MIN',
    30: 'ANCHOR_ARRIVE_BIN',
    31: 'TOUR_DUR_HR',
    32: 'TOUR_DUR_MIN',
    33: 'TOUR_DUR_BIN',
    34: 'MAJOR_UNIV_DEST',
    35: 'SPEC_EVENT_DEST',
    36: 'IS_SUBTOUR',
    37: 'PARENT_TOUR_ID',
    38: 'PARENT_TOUR_MODE',
    39: 'NUM_SUBTOURS',
    40: 'CHILD_TOUR_ID_1',
    41: 'CHILD_TOUR_ID_2',
    42: 'CHILD_TOUR_ID_3',
    43: 'ESCORTED_TOUR',
    44: 'CHAUFFUER_ID',
    45: 'ESCORTING_TOUR',
    46: 'NUM_PERSONS_ESCORTED',
    47: 'ESCORT_PERS_1',
    48: 'ESCORT_PERS_2',
    49: 'ESCORT_PERS_3',
    50: 'ESCORT_PERS_4',
    51: 'ESCORT_PERS_5',
    52: 'OUTBOUND_STOPS',
    53: 'INBOUND_STOPS',
    54: 'OSTOP_1_PLACENO',
    55: 'OSTOP_1_X',
    56: 'OSTOP_1_Y',
    57: 'OSTOP_1_TAZ',
    58: 'OSTOP_1_MAZ',
    59: 'OSTOP_1_ARR_HR',
    60: 'OSTOP_1_ARR_MIN',
    61: 'OSTOP_1_ARR_BIN',
    62: 'OSTOP_1_DEP_HR',
    63: 'OSTOP_1_DEP_MIN',
    64: 'OSTOP_1_DEP_BIN',
    65: 'OSTOP_1_DUR_HR',
    66: 'OSTOP_1_DUR_MIN',
    67: 'OSTOP_1_DUR_BIN',
    68: 'OSTOP_1_PURP',
    69: 'OSTOP_1_MODE',
    70: 'OSTOP_1_ESCORT_ID',
    71: 'OSTOP_1_PUDO',
    72: 'OSTOP_1_MAJUNIV',
    73: 'OSTOP_1_SPECEVENT',
    74: 'OSTOP_2_PLACENO',
    75: 'OSTOP_2_X',
    76: 'OSTOP_2_Y',
    77: 'OSTOP_2_TAZ',
    78: 'OSTOP_2_MAZ',
    79: 'OSTOP_2_ARR_HR',
    80: 'OSTOP_2_ARR_MIN',
    81: 'OSTOP_2_ARR_BIN',
    82: 'OSTOP_2_DEP_HR',
    83: 'OSTOP_2_DEP_MIN',
    84: 'OSTOP_2_DEP_BIN',
    85: 'OSTOP_2_DUR_HR',
    86: 'OSTOP_2_DUR_MIN',
    87: 'OSTOP_2_DUR_BIN',
    88: 'OSTOP_2_PURP',
    89: 'OSTOP_2_MODE',
    90: 'OSTOP_2_ESCORT_ID',
    91: 'OSTOP_2_PUDO',
    92: 'OSTOP_2_MAJUNIV',
    93: 'OSTOP_2_SPECEVENT',
    94: 'OSTOP_3_PLACENO',
    95: 'OSTOP_3_X',
    96: 'OSTOP_3_Y',
    97: 'OSTOP_3_TAZ',
    98: 'OSTOP_3_MAZ',
    99: 'OSTOP_3_ARR_HR',
    100: 'OSTOP_3_ARR_MIN',
    101: 'OSTOP_3_ARR_BIN',
    102: 'OSTOP_3_DEP_HR',
    103: 'OSTOP_3_DEP_MIN',
    104: 'OSTOP_3_DEP_BIN',
    105: 'OSTOP_3_DUR_HR',
    106: 'OSTOP_3_DUR_MIN',
    107: 'OSTOP_3_DUR_BIN',
    108: 'OSTOP_3_PURP',
    109: 'OSTOP_3_MODE',
    110: 'OSTOP_3_ESCORT_ID',
    111: 'OSTOP_3_PUDO',
    112: 'OSTOP_3_MAJUNIV',
    113: 'OSTOP_3_SPECEVENT',
    114: 'OSTOP_4_PLACENO',
    115: 'OSTOP_4_X',
    116: 'OSTOP_4_Y',
    117: 'OSTOP_4_TAZ',
    118: 'OSTOP_4_MAZ',
    119: 'OSTOP_4_ARR_HR',
    120: 'OSTOP_4_ARR_MIN',
    121: 'OSTOP_4_ARR_BIN',
    122: 'OSTOP_4_DEP_HR',
    123: 'OSTOP_4_DEP_MIN',
    124: 'OSTOP_4_DEP_BIN',
    125: 'OSTOP_4_DUR_HR',
    126: 'OSTOP_4_DUR_MIN',
    127: 'OSTOP_4_DUR_BIN',
    128: 'OSTOP_4_PURP',
    129: 'OSTOP_4_MODE',
    130: 'OSTOP_4_ESCORT_ID',
    131: 'OSTOP_4_PUDO',
    132: 'OSTOP_4_MAJUNIV',
    133: 'OSTOP_4_SPECEVENT',
    134: 'ISTOP_1_PLACENO',
    135: 'ISTOP_1_X',
    136: 'ISTOP_1_Y',
    137: 'ISTOP_1_TAZ',
    138: 'ISTOP_1_MAZ',
    139: 'ISTOP_1_ARR_HR',
    140: 'ISTOP_1_ARR_MIN',
    141: 'ISTOP_1_ARR_BIN',
    142: 'ISTOP_1_DEP_HR',
    143: 'ISTOP_1_DEP_MIN',
    144: 'ISTOP_1_DEP_BIN',
    145: 'ISTOP_1_DUR_HR',
    146: 'ISTOP_1_DUR_MIN',
    147: 'ISTOP_1_DUR_BIN',
    148: 'ISTOP_1_PURP',
    149: 'ISTOP_1_MODE',
    150: 'ISTOP_1_ESCORT_ID',
    151: 'ISTOP_1_PUDO',
    152: 'ISTOP_1_MAJUNIV',
    153: 'ISTOP_1_SPECEVENT',
    154: 'ISTOP_2_PLACENO',
    155: 'ISTOP_2_X',
    156: 'ISTOP_2_Y',
    157: 'ISTOP_2_TAZ',
    158: 'ISTOP_2_MAZ',
    159: 'ISTOP_2_ARR_HR',
    160: 'ISTOP_2_ARR_MIN',
    161: 'ISTOP_2_ARR_BIN',
    162: 'ISTOP_2_DEP_HR',
    163: 'ISTOP_2_DEP_MIN',
    164: 'ISTOP_2_DEP_BIN',
    165: 'ISTOP_2_DUR_HR',
    166: 'ISTOP_2_DUR_MIN',
    167: 'ISTOP_2_DUR_BIN',
    168: 'ISTOP_2_PURP',
    169: 'ISTOP_2_MODE',
    170: 'ISTOP_2_ESCORT_ID',
    171: 'ISTOP_2_PUDO',
    172: 'ISTOP_2_MAJUNIV',
    173: 'ISTOP_2_SPECEVENT',
    174: 'ISTOP_3_PLACENO',
    175: 'ISTOP_3_X',
    176: 'ISTOP_3_Y',
    177: 'ISTOP_3_TAZ',
    178: 'ISTOP_3_MAZ',
    179: 'ISTOP_3_ARR_HR',
    180: 'ISTOP_3_ARR_MIN',
    181: 'ISTOP_3_ARR_BIN',
    182: 'ISTOP_3_DEP_HR',
    183: 'ISTOP_3_DEP_MIN',
    184: 'ISTOP_3_DEP_BIN',
    185: 'ISTOP_3_DUR_HR',
    186: 'ISTOP_3_DUR_MIN',
    187: 'ISTOP_3_DUR_BIN',
    188: 'ISTOP_3_PURP',
    189: 'ISTOP_3_MODE',
    190: 'ISTOP_3_ESCORT_ID',
    191: 'ISTOP_3_PUDO',
    192: 'ISTOP_3_MAJUNIV',
    193: 'ISTOP_3_SPECEVENT',
    194: 'ISTOP_4_PLACENO',
    195: 'ISTOP_4_X',
    196: 'ISTOP_4_Y',
    197: 'ISTOP_4_TAZ',
    198: 'ISTOP_4_MAZ',
    199: 'ISTOP_4_ARR_HR',
    200: 'ISTOP_4_ARR_MIN',
    201: 'ISTOP_4_ARR_BIN',
    202: 'ISTOP_4_DEP_HR',
    203: 'ISTOP_4_DEP_MIN',
    204: 'ISTOP_4_DEP_BIN',
    205: 'ISTOP_4_DUR_HR',
    206: 'ISTOP_4_DUR_MIN',
    207: 'ISTOP_4_DUR_BIN',
    208: 'ISTOP_4_PURP',
    209: 'ISTOP_4_MODE',
    210: 'ISTOP_4_ESCORT_ID',
    211: 'ISTOP_4_PUDO',
    212: 'ISTOP_4_MAJUNIV',
    213: 'ISTOP_4_SPECEVENT',
    214: 'PEREXPFACT',
    215: 'HHEXPFACT',
    216: 'PERSONTYPE',
    217: 'FULLY_JOINT',
    218: 'PARTIAL_TOUR',
    219: 'JTOUR_ID',
    220: 'ERROR',
    221: 'JOINT_STATUS',
    222: 'JOINT_TOUR_PURP',
    223: 'DIST',
    224: 'OUT_ESCORT_TYPE',
    225: 'OUT_CHAUFFUER_ID',
    226: 'OUT_CHAUFFUER_PURP',
    227: 'OUT_CHAUFFUER_PTYPE',
    228: 'INB_ESCORT_TYPE',
    229: 'INB_CHAUFFUER_ID',
    230: 'INB_CHAUFFUER_PURP',
    231: 'INB_CHAUFFUER_PTYPE',
    232: 'OUT_ESCORTING_TYPE',
    233: 'INB_ESCORTING_TYPE',
    234: 'OUT_ESCORTEE_TOUR_PURP',
    235: 'INB_ESCORTEE_TOUR_PURP',
    236: 'OUT_ESCORTING_EPISODES',
    237: 'INB_ESCORTING_EPISODES'
}

################## Function Definitions ##########################################

def distance_on_unit_sphere(lat1, long1, lat2, long2):
    """source: http://www.johndcook.com/python_longitude_latitude.html """
    # returns distance in mile

    # Convert latitude and longitude to
    # spherical coordinates in radians.
    degrees_to_radians = math.pi/180.0

    # phi = 90 - latitude
    phi1 = (90.0 - lat1)*degrees_to_radians
    phi2 = (90.0 - lat2)*degrees_to_radians

    # theta = longitude
    theta1 = long1*degrees_to_radians
    theta2 = long2*degrees_to_radians

    # Compute spherical distance from spherical coordinates.

    # For two locations in spherical coordinates
    # (1, theta, phi) and (1, theta, phi)
    # cosine( arc length ) =
    #    sin phi sin phi' cos(theta-theta') + cos phi cos phi'
    # distance = rho * arc length

    cos = (math.sin(phi1)*math.sin(phi2)*math.cos(theta1 - theta2) +
           math.cos(phi1)*math.cos(phi2))
    arc = math.acos( cos )

    # Multiply arc by the radius of the earth in feet
    return arc*3960

def calculate_duration(start_hr, start_min, end_hr, end_min):
    #TODO: check how time is coded when clock goes over 12am into the next time
    if(start_hr < START_OF_DAY_MIN/60):
        start_hr = start_hr + 24 # adjust to next day
    if(end_hr < START_OF_DAY_MIN/60):
        end_hr = end_hr + 24
    start_time = start_hr*60+start_min  #convert to minutes
    end_time = end_hr*60+end_min        #convert to minutes
    total_minutes = end_time - start_time
    dur_hr = total_minutes//60
    dur_min = total_minutes % 60
    return (dur_hr, dur_min)

def convert2minutes(hours,minutes):
    """ given a duration of (hours, minutes), return the equivalent in minutes """
    return hours*60+minutes

def convert2bin(hour,minute):
    """ given a time specified as hour:minute, return the equivalent in time window bins"""
    min_from_start_of_day = convert2minutes(hour,minute) - START_OF_DAY_MIN
    if min_from_start_of_day<0:
        min_from_start_of_day = min_from_start_of_day + 60*24   #add another day

    bin_number = 1+ math.floor(min_from_start_of_day / TIME_WINDOW_BIN_MIN)    #minutes from 'start of the say' divided by width of a bin gives the bin number
    return bin_number

def add_quote_char(string):
    return '"'+string+'"'

def print_in_separate_files(hh_list, out_dir):
    #specify output files
    hh_file = open(out_dir+'clean_hh/households.csv', 'w')
    per_file = open(out_dir+'clean_hh/persons.csv', 'w')
    trip_file = open(out_dir+'clean_hh/trips.csv', 'w')
    tour_file = open(out_dir+'clean_hh/tours.csv', 'w')
    joint_trip_file = open(out_dir+'clean_hh/joint_ultrips.csv', 'w')
    unique_jtrip_file = open(out_dir+'clean_hh/unique_joint_ultrips.csv', 'w')
    unique_jtour_file = open(out_dir+'clean_hh/unique_joint_tours.csv', 'w')

    err_log_file = open(out_dir+'error_log.txt', 'w')
    problem_hh_file = open(out_dir+'err_hh/households_with_err.csv', 'w')
    problem_per_file = open(out_dir+'err_hh/persons_from_err_hh.csv', 'w')
    problem_trip_file = open(out_dir+'err_hh/trips_from_err_hh.csv', 'w')
    problem_tour_file = open(out_dir+'err_hh/tours_from_err_hh.csv', 'w')
    problem_joint_trip_file = open(out_dir+'err_hh/joint_ultrips_from_err_hh.csv', 'w')
    problem_unique_jtrip_file = open(out_dir+'err_hh/unique_joint_ultrips_from_err_hh.csv', 'w')
    problem_unique_jtour_file = open(out_dir+'err_hh/unique_joint_tours_from_err_hh.csv', 'w')
    recode_log_file = open(out_dir+'recode_log.txt', 'w')

    #print column headers in tables
    Household.print_header(hh_file)
    Person.print_header(per_file)
    Trip.print_header(trip_file)
    Tour.print_header(tour_file)
    Joint_ultrip.print_header(joint_trip_file)
    Joint_ultrip.print_header_unique(unique_jtrip_file)
    Joint_tour.print_header(unique_jtour_file)

    Household.print_header(problem_hh_file)
    Person.print_header(problem_per_file)
    Trip.print_header(problem_trip_file)
    Tour.print_header(problem_tour_file)
    Joint_ultrip.print_header(problem_joint_trip_file)
    Joint_ultrip.print_header_unique(problem_unique_jtrip_file)
    Joint_tour.print_header(problem_unique_jtour_file)

    num_err_hh = 0
    for hh in hh_list:
        #print log of all recodes
        hh.print_recode_tags(recode_log_file)

        #households with error tag go to one set of files
        if hh.error_flag==True:
            #household contains error: print error messages to one file; print trips/j-trips to a separate trip file
            num_err_hh = num_err_hh+1
            hh.print_tags(err_log_file)
            hh.print_vals(problem_hh_file)
            hh.print_joint_trips(problem_joint_trip_file)
            hh.print_unique_joint_trips(problem_unique_jtrip_file)
            hh.print_unique_joint_tours(problem_unique_jtour_file)
            for psn in hh.persons:
                psn.print_vals(problem_per_file)
                for tour in psn.tours:
                    for trip in tour.trips:
                        trip.print_vals(problem_trip_file)
                    tour.print_vals(problem_tour_file)
        else:
            #'clean' households go to another set of files
            hh.print_vals(hh_file)
            hh.print_joint_trips(joint_trip_file)
            hh.print_unique_joint_trips(unique_jtrip_file)
            hh.print_unique_joint_tours(unique_jtour_file)
            for psn in hh.persons:
                psn.print_vals(per_file)
                for tour in psn.tours:
                    for trip in tour.trips:
                        trip.print_vals(trip_file)
                    tour.print_vals(tour_file)

    hh_file.close()
    per_file.close()
    trip_file.close()
    tour_file.close()
    joint_trip_file.close()
    unique_jtrip_file.close()
    unique_jtour_file.close()

    err_log_file.close()
    problem_hh_file.close()
    problem_per_file.close()
    problem_trip_file.close()
    problem_tour_file.close()
    problem_joint_trip_file.close()
    problem_unique_jtrip_file.close()
    recode_log_file.close()

    print("{} households processed.".format(len(hh_list)))
    print("{} households contain error.".format(num_err_hh))

def print_in_same_files(hh_list, out_dir):
    """ print problematic records (relating to joint travel) in the same files as the 'clean' records """
    #specify output files
    hh_file = open(out_dir+'households.csv', 'w')
    per_file = open(out_dir+'persons.csv', 'w')
    trip_file = open(out_dir+'trips.csv', 'w')
    joint_trip_file = open(out_dir+'joint_ultrips.csv', 'w')
    unique_jtrip_file = open(out_dir+'unique_joint_ultrips.csv', 'w')
    tour_file = open(out_dir+'tours.csv', 'w')
    unique_jtour_file = open(out_dir+'unique_joint_tours.csv', 'w')

    err_log_file = open(out_dir+'error_log.txt', 'w')
    recode_log_file = open(out_dir+'recode_log.txt', 'w')

    #print column headers in tables
    Trip.print_header(trip_file)
    Joint_ultrip.print_header(joint_trip_file)
    Joint_ultrip.print_header_unique(unique_jtrip_file)
    Tour.print_header(tour_file)
    Joint_tour.print_header(unique_jtour_file)
    Person.print_header(per_file)
    Household.print_header(hh_file)

    num_err_perons = 0
    num_err_hh = 0
    count_persons = 0
    count_tours = 0
    count_trips = 0


    for hh in hh_list:
        #print log of all recodes
        hh.print_recode_tags(recode_log_file)

        if hh.error_flag==True:
            num_err_hh = num_err_hh+1
            hh.print_tags(err_log_file)

        hh.print_vals(hh_file)
        hh.print_joint_trips(joint_trip_file)
        hh.print_unique_joint_trips(unique_jtrip_file)
        hh.print_unique_joint_tours(unique_jtour_file)
        for psn in hh.persons:
            count_persons = count_persons+1
            psn.print_vals(per_file)
            if psn.error_flag==True:
                num_err_perons = num_err_perons+1
            for tour in psn.tours:
                count_tours = count_tours+1
                tour.print_vals(tour_file)
                for trip in tour.trips:
                    count_trips = count_trips+1
                    trip.print_vals(trip_file)

    hh_file.close()
    per_file.close()
    trip_file.close()
    joint_trip_file.close()
    tour_file.close()
    unique_jtrip_file.close()
    unique_jtour_file.close()
    err_log_file.close()
    recode_log_file.close()

    print("Processed {} households, {} individuals, {} person-trips, {} person-tours.".format(len(hh_list), count_persons, count_trips, count_tours))
    print("{} households contain error.".format(num_err_hh))
    print("{} persons contain error.".format(num_err_perons))

################## Class definitions ##########################################

class Household:
    """ Household class """
    def __init__(self, hh_id, area):
        self.hh_id = hh_id
        self.persons = []                           #TODO: could use OrderedDict instead for possibly improved efficiency
        self.joint_episodes = defaultdict(list)     #dict that allow multiple values for each key
                                                #trip departure time (in minutes) is mapped to a list of Joint_trip objects
        self.unique_jt_groups = []              #each list element corresponds to trips jointly made by a travel group
        self.unique_jtours = []                 #each list element corresponds to a Joint_tour object
        self.error_flag = False
        self.tags = []
        self.recode_tags = []
        self.area = area

    def print_header(fp):
        _header=["HH_ID", "NUM_PERS", "AREA"]
        fp.write(','.join(['%s' %field for field in _header])+'\n')

    def print_vals(self, fp):
        _vals = [self.hh_id, len(self.persons), self.area]
        fp.write(','.join(['%s' %v for v in _vals])+'\n')

    def get_id(self):
        return self.hh_id

    def add_person(self, person):
        self.persons.append(person)

    def add_joint_episodes(self, joint_episodes):
        for _jt in joint_episodes:
            self.joint_episodes[_jt.get_depart_time()].append(_jt)

    def get_person(self, per_id):
        """return the Person object with the given person id"""
        for _person in self.persons:
            if _person.per_id==per_id:
                return _person

    def log_recode(self, msg):
        self.recode_tags.append(msg)

    def log_warning(self, msg):
        self.tags.append(msg)

    def log_error(self, err_msg=None):
        self.error_flag = True
        if err_msg:
            self.tags.append(err_msg)

    def print_tags(self, fp):
        fp.write("HH_ID={}\n".format(self.get_id()))
        for _tag in self.tags:
            fp.write("\t"+_tag+"\n")

    def print_recode_tags(self, fp):
        for _tag in self.recode_tags:
            fp.write("HH_ID={}\t".format(self.get_id()))
            fp.write(_tag+"\n")

    def _error_num_joint_episodes(self, jt_list):
        """ check if there are enough joint trips to make up the reported travel group(s)  """
        """ joint trips in the list are assumed to have been sorted by travel party        """
        _start_ix = int(0)
        _max_ix = len(jt_list)
        _error = False
        #joint trip objects departing at this time do not necessarily correspond to the same travel episode
        while _start_ix<_max_ix:
            #given the reported travel party size of the current joint trip object
            #check if there are this many joint trips remaining in the list
            _size = jt_list[_start_ix].number_hh #size of travel party
            _stop_ix = int(_start_ix + _size)
            if _stop_ix>_max_ix:
                #there are fewer joint trip entries than expected
                _error = True
                break;
            _start_ix = _stop_ix

        return _error


    def process_joint_episodes(self):
        _dep_time_list = sorted(self.joint_episodes.keys())  #sorted list of joint-trip departure times as key values
        #process each dep_time entry at a time
        #assign each consistent group of unlinked trip entries a unique joint trip id
        _joint_ultrip_id = 0
        #_d = 0                          #index into _dep_time_list
        _d = -1                          #index into _dep_time_list
        _d_max = len(_dep_time_list)

        #while _d < _d_max:
        while (_d+1) < _d_max:
            #move list index on to the next departure time group
            _d = _d+1

            _dep_time = _dep_time_list[_d]
            _jt_list = self.joint_episodes[_dep_time]          #_jt_list includes all household members' joint travels departing at this time

            _jt_list.sort(key=lambda jt: jt.travel_party)   #sort joint trip objects by reported travel party
            _error_merge = False

            #
            # Attempt to resolve inconsistency in reported departure times by merging trip entries departing within the error time buffer
            # Assuming that reported number of participants are accurate
            #
            if self._error_num_joint_episodes(_jt_list):   #number of joint trips not matching up with the travel size(s) reported
                #try merge with the next lot of trips of "similar" departure time
                """
                _error_merge = True #<--remove this once counting is done and uncommented the following if-else!
                """
                if (_d+1) < _d_max:
                    _next_dep_time = _dep_time_list[_d+1]
                    if (_next_dep_time-_dep_time)<=NEGLIGIBLE_DELTA_TIME:
                        _tmp_jt_list = _jt_list.copy()
                        _tmp_jt_list.extend(self.joint_episodes[_next_dep_time])                #try merge two lists
                        _tmp_jt_list.sort(key=lambda jt: (jt.travel_party, jt.depart_time))     #sort by reported travel party THEN by departure time
                        #check if the merged list has enough trip records
                        if not self._error_num_joint_episodes(_tmp_jt_list):
                            self.log_recode("Inconsistent departure times: trips departing at {}:{} and {}:{} are assumed to be made together".format(_dep_time//60,_dep_time%60,
                                                                                                                                                      _next_dep_time//60,_next_dep_time%60))
                            #merged list has enough trip records, replace _jt_list with the merged list
                            _jt_list = _tmp_jt_list
                            #advance down the departure time list
                            _d = _d+1
                            _dep_time = _next_dep_time
                        else:
                            #merging did not help resolve the issue
                            _error_merge = True
                    else:
                        #departure times not close enough for merging
                        _error_merge = True
                else:
                    #no trips to be considered for merging
                    _error_merge = True


            if _error_merge:
                #there are fewer joint trip entries than expected
                msg = "Number of joint trips departing at {}:{} differs from at least one of the reported travel group size".format(_dep_time//60,_dep_time%60)
                #self.log_error()
                for _jt in _jt_list:
                    _jt.log_error(msg)
                continue

            #
            # Scan through the joint trips to identify any errors in reported number of hh members or reported travel participants
            #
            _start_ix = int(0)
            _max_ix = len(_jt_list)
            _error_travel_party = False
            _error_number_hh = False
            #joint trip objects departing at this time do not necessarily correspond to the same travel episode
            while _start_ix<_max_ix:
                _size = _jt_list[_start_ix].number_hh   #number of hh members reported by the entry of the group
                _stop_ix = int(_start_ix + _size)
                #make pair-wise comparison among joint trip entries of the current travel group to verify consistency
                for _i in range(_start_ix, _stop_ix-1):
                    if _jt_list[_i].number_hh == _jt_list[_i+1].number_hh:
                    #reported number of travelers is consistent
                        if _jt_list[_i].travel_party == _jt_list[_i+1].travel_party:
                            #reported travel party is consistent
                            pass
                        else:
                            #reported travel party is inconsistent
                            _error_travel_party = True
                    else:
                    #reported number of travelers is inconsistent
                        _error_number_hh = True


                # Attempt to resolve inconsistency in reported travel party
                # Assuming that reported departure times and number of participants are accurate
                if _error_travel_party & (not _error_number_hh):

                    """
                    ##remove the lines below before uncommenting
                    msg = "Joint trips departing at {}:{} reported inconsistent participants".format(_dep_time//60,_dep_time%60)
                    for _jt in _jt_list:
                        _jt.log_error(msg)
                    break

                    """
                    # merge the travel party entries across all joint trips
                    # if the merged set has the same number of participants as reported in number_hh
                    # then use the merged set as the 'true' set to continue
                    # otherwise, log as an error and break from loop
                    _set_participants = set(_jt_list[_start_ix].travel_party)   #union of the reported participants
                    _set_persons = {_jt_list[_start_ix].get_per_id()}        #ID of persons who made the trips
                    for _i in range(_start_ix+1, _stop_ix):
                        _set_participants |= set(_jt_list[_i].travel_party)
                        _set_persons |= {_jt_list[_i].get_per_id()}
                    if len(_set_participants)==_jt_list[_start_ix].number_hh:
                        #use the merged set of participants as the 'true' set
                        for _i in range(_start_ix, _stop_ix):
                            _jt_list[_i].recode_travel_party(_set_participants)
                    elif len(_set_persons)==_jt_list[_start_ix].number_hh:
                        #use the list of persons who made the trips as the 'true' set
                        for _i in range(_start_ix, _stop_ix):
                            _jt_list[_i].recode_travel_party(_set_persons)
                    else:
                        msg = "Joint trips departing at {}:{} reported inconsistent participants".format(_dep_time//60,_dep_time%60)
                        #self.log_error(msg)
                        for _jt in _jt_list:
                            _jt.log_error(msg)
                        break


                # Attempt to resolve inconsistency in reported number of participants
                # Assuming that reported departure times and travel participants are accurate
                if _error_number_hh & (not _error_travel_party):
                    """
                    msg = "Joint trips departing at {}:{} reported inconsistent number of participants".format(_dep_time//60,_dep_time%60)
                    for _jt in _jt_list:
                        _jt.log_error(msg)
                    break
                    """
                    # 'fix' the incorrect number_hh and continue
                    _number_travelers = len(_jt_list[_start_ix].travel_party)
                    for _i in range(_start_ix, _stop_ix):
                        _jt_list[_i].recode_number_travelers(_number_travelers)


                #inconsistencies found in both reported number of participants and reported travel participants
                if _error_number_hh & _error_travel_party:
                    # log as an error and break from loop, for now
                    msg = "Joint trips departing at {}:{} reported inconsistent info about participants & travel group sizes".format(_dep_time//60,_dep_time%60)
                    #self.log_error(msg)
                    for _jt in _jt_list:
                        _jt.log_error(msg)
                    break

                #if reach this point, a consistent subset of joint episode entries have been found for a travel group
                #increase the id value by 1
                _joint_ultrip_id = _joint_ultrip_id+1

                #add the current slice of joint episode entries as a joint travel episode/group
                self.unique_jt_groups.append(_jt_list[_start_ix:_stop_ix])

                #look up the driver of the group, if any
                #also, check presence of adult and children in travel party
                _driver_ix = None    #index where the driver of the joint travel party is found
                _num_adults = 0
                for _i in range(_start_ix, _stop_ix):
                    if _jt_list[_i].driver>0:
                        _driver_ix = _i
                    _num_adults = _num_adults + int(_jt_list[_i].parent_trip.per_obj.get_is_adult())

                #TODO: verify that a driver is indeed found for an auto joint trip

                #derive travel party composition
                _composition = NewCompType["MIXED"]
                if _num_adults==_size:
                    #number of adults equals number of people in the travel party
                    _composition = NewCompType["ALL_ADULTS"]
                elif _num_adults==0:
                    #no adults
                    _composition = NewCompType["ALL_CHILDREN"]

                #go back and update the joint-unlinked-trip records
                for _i in range(_start_ix, _stop_ix):
                    #joint ultrip id
                    _jt_list[_i].set_joint_ultrip_id(_joint_ultrip_id)
                    #chauffuer id
                    if (_driver_ix!=None) & (_i!=_driver_ix):  #skip the driver him/her-self
                        _jt_list[_i].set_chauffuer(_jt_list[_driver_ix].driver)
                    #travel group composition
                    _jt_list[_i].set_composition(_composition)

                #point to the start of the next group of joint trips
                _start_ix = _stop_ix
            #end-while loop for processing each travel group
        #end-for loop for processing each departure time
        #now, go through all the person trips and determine all joint legs of a given trip are properly joint
        for p in self.persons:
            for tour in p.tours:
                for trip in tour.trips:
                    trip.set_jt_grouping_status()


    def process_joint_tours(self):
        """determine partially vs fully joint tour"""
        #a tour is fully joint if all the constituting trips are joint-grouped and made by the same group of people

        _jt_dict = dict()       #dictionary with jt_id as the key and ([per ids],[tour ids],[trip ids], [tour lengths]) as the value
        _start_jtour = -1       #index into unique_jt_groups that marks the start of potentially a fully joint tour
        _end_jtour   = -1       #index into unique_jt_groups that marks the end of a fully joint tour
        _num_seq_jtrips = 0      #number of sequential joint trips found so far in a joint tour
        _num_jtours = 0         #number of joint tours found so far
        _prev_jt = ([],[])
        for _jt_group_idx, _cur_jt_group in enumerate(self.unique_jt_groups):
            #_cur_jt_group is a list of joint episodes that were made together by household members

            #first check if the parent trips are all joint and grouped. if not, stop processing this group of joint travel
            _all_grouped = True
            for _jt in _cur_jt_group:
                if not (_jt.parent_trip.get_joint_status()==NewJointCategory['JOINT-GROUPED']):
                    _all_grouped = False
            if not _all_grouped:
                continue

            #all person trips involved are grouped. now check how they match up at the tour level
            #refresh lists for the current group of joint trips
            _pids = []
            _tour_ids = []
            _trip_ids = []
            _tour_lens = []
            for _jt in _cur_jt_group:
                #_jt is a Joint_ultrip object associated with a person in the travel party
                _pids.append(_jt.parent_trip.get_per_id())
                _tour_ids.append(_jt.parent_trip.get_tour_id())
                _trip_ids.append(_jt.parent_trip.get_id())
                _tour_lens.append(len(_jt.parent_trip.tour_obj.trips))

            #necessary condition for a joint episode group to be the start of a joint tour:
            #(1) trip_id the same and equal to 1 for all participants
            #(2) length of corresponding tour is the same for all participants
            if (len(set(_trip_ids))==1) & (_trip_ids[0]==1) & (len(set(_tour_lens))==1):
                _start_jtour = _jt_group_idx
                _num_seq_jtrips = 1
            #necessary condition for a joint episode group to be part of the same joint tour as the previous joint episode:
            #(1)count of sequential number of joint episode is greater than 0
            #(2)identical set of person id and tour id as the previous joint episode
            #(3)trip_id the same for all participants and all equal to the previous trip's id plus 1
            elif (_num_seq_jtrips>0) & ((_pids, _tour_ids)==_prev_jt) & (len(set(_trip_ids))==1) & (_trip_ids[0]==_num_seq_jtrips+1):
                _num_seq_jtrips = _num_seq_jtrips + 1
                if _num_seq_jtrips==_tour_lens[0]:
                    #this is the last episode in the joint tour
                    _end_jtour = _jt_group_idx
                    _num_jtours = _num_jtours + 1
                    self.set_joint_tour(_num_jtours, self.unique_jt_groups[_start_jtour:_end_jtour+1])
                    #reset markers
                    _start_jtour = -1
                    _end_jtour   = -1
                    _num_seq_jtrips = 0
            else:   #this joint trip is part of a partially joint tour
                #reset markers
                _start_jtour = -1
                _end_jtour   = -1
                _num_seq_jtrips = 0

            _prev_jt = (_pids, _tour_ids)

        #now that all fully joint tours have been identified, go back and classify each tour as one of the following:
        # (1) independent tours:        all trips.JOINT==NOT-JOINT
        # (2) partially-joint tours:    all trips.JOINT<>JOINT; some are FULLY-JOINT, some are NOT-JOINT
        # (3) fully-joint tours:        tour.get_is_fully_joint()==True
        # (4) partially-joint problematic tours: some trips are NOT-JOINT, some are JOINT (not grouped)
        # (5) jointness-unclear tours :    no NOT-JOINT, some are JOINT
        for psn in self.persons:
            for tour in psn.tours:
                tour.set_joint_status()

    def set_joint_tour(self, jtour_id, jt_groups):
        """label join trips in the jt_groups list as being part of a fully joint tour with an id of jtour_id """
        for _jt_group in jt_groups:
            for _jt in _jt_group:
                _jt.set_jtour_id(jtour_id)
        self.unique_jtours.append(Joint_tour(jtour_id, jt_groups))

    def process_escort_trips(self):
        #scan through each unique joint episode group
        for _jt_group in self.unique_jt_groups:
            #find the driver in the group, if any
            _driver = set([_jt.driver for _jt in _jt_group])
            _num_driver = len(_driver)
            _driver_id = list(_driver)[0]
            _escorted_pers = set()      #ID of hh members who are escorted
            _escorting_jt = set()       #Joint_ultrip objects of hh members who are not the driver but also escorting

            if (_num_driver==1) & (_driver_id>0):   #TODO: prevent driver id==nan
                #there is exactly one driver on this unlinked joint trip
                #locate the driver's joint trip to see if joint trip was associated with a drop-off or pick-up activity
                _drop_off = False
                _pick_up  = False
                _driver_jt = None
                for _jt in _jt_group:
                    if _jt.get_per_id()==_driver_id:
                        #found driver's trip
                        _drop_off = _jt.get_dest_escort_pudo()==NewEscort['DROP_OFF']
                        _pick_up =  _jt.get_orig_escort_pudo()==NewEscort['PICK_UP']
                        _driver_jt = _jt
                        break

                if _drop_off:
                    #if trip was for dropping off
                    #scan through joint trip record for the passengers
                    for _jt in _jt_group:
                        if _jt.parent_trip.get_is_escorting():
                            #hh member who is escorting (could be the driver)
                            _escorting_jt.add(_jt)
                        else:
                            #found a person being dropped off
                            _escorted_pers.add(_jt.get_per_id())    #add person to the driver's set of escorted persons
                            #set person's trip as being dropped off at destination
                            _jt.parent_trip.set_escorted(NewEscort['DROP_OFF'])

                if _pick_up:
                    #if trip was for picking up, the person(s) being picked up is given by
                    #the difference between the travel party at the origin and travel party at the destination of the current trip
                    #note that the previous trip may not be a joint travel
                    _prev_set = set(_driver_jt.get_orig_travel_party())
                    _cur_set = set(_driver_jt.get_travel_party())
                    _set_diff = _cur_set - _prev_set         #persons being picked up at trip origin
                    _escorted_pers = _escorted_pers|_set_diff
                    #set escorted person's trip as being picked up at origin
                    for _jt in _jt_group:
                        if _jt.get_per_id() in _set_diff:
                            #found a person being picked up
                            _jt.parent_trip.set_escorted(NewEscort['PICK_UP'])
                        else:
                            #otherwise person is escorting -> add to the set of escorting trips
                            _escorting_jt.add(_jt)

                if _driver_id in _escorted_pers:
                    _escorted_pers.remove(_driver_id)

                #set escorting-related fields in driver's and any other escorting individuals' trip
                if _drop_off:
                    _pudo = NewEscort['DROP_OFF']
                elif _pick_up:
                    _pudo = NewEscort['PICK_UP']
                else:
                    _pudo = NewEscort['NEITHER']
                for _jt in _escorting_jt:
                    _jt.parent_trip.set_escorting(_pudo, _escorted_pers)

            elif _num_driver>1:
                self.log_error("more than one driver found for joint trip #{}".format(_jt_group[0].jt_id))

    def process_escort_tours(self):
        for _per in self.persons:
            for _tour in _per.tours:
                _tour.set_escorting_fields()
                _tour.set_escorted_fields()

    def print_joint_trips(self, fp):
        _dep_time_list = sorted(self.joint_episodes.keys())  #sorted list of departure times as key values
        #print in order of dep_time
        for _dep_time in _dep_time_list:
            _jt_list = self.joint_episodes[_dep_time]
            for _jt in _jt_list:
                _jt.print_vals(fp)

    def print_unique_joint_trips(self, fp):
        for _jt_gp in self.unique_jt_groups:    #each item in the list is itself a list of joint trip objects
            _jt_gp[0].print_vals_unique(fp)     #print from the first joint trip (the same if any arbitrary joint trip in the group)

    def print_unique_joint_tours(self, fp):
        for _jtour in self.unique_jtours:       #each item in the list is a Joint_tour object
            _jtour.print_vals(fp)


class Person:
    """ Person class """
    def __init__(self, hh, per_id, df):
        self.hh_obj = hh
        self.hh_obj.add_person(self)     #can start logging errors/warning
        self.per_id = per_id
        self.fields = {'HH_ID': hh.get_id(), 'PER_ID': per_id}
        self.set_per_type(self._calc_per_type(df)) #errors are logged to the hh obj
        self.tours = []
        self.error_flag = False

    def print_header(fp):
        _header=["HH_ID", "PER_ID", "PERSONTYPE", "AGE_CAT", "EMPLY", "HOURS_CAT", "EMP_CAT", "STUDE", "SCHOL", "STU_CAT", "PERSONTYPE0", "EMP_CAT0", "STU_CAT0", "ERROR" ]
        fp.write(','.join(['%s' %field for field in _header])+'\n')

    def print_vals(self, fp):
        _fields = defaultdict(lambda: '', self.fields)
        _vals = [_fields['HH_ID'], _fields['PER_ID'], _fields['PERSONTYPE'],
                 _fields['AGE_CAT'], _fields['EMPLY'], _fields['HOURS_CAT'],
                 _fields['EMP_CAT'], _fields['STUDE'], _fields['SCHOL'], _fields['STU_CAT'],
                 _fields["PERSONTYPE0"], _fields["EMP_CAT0"], _fields["STU_CAT0"], add_quote_char(_fields["ERROR"]) ]
        fp.write(','.join(['%s' %v for v in _vals])+'\n')

    def _calc_emp_cat(self, age, emply):
        _emp = np.NAN
        if (age>=3) & (emply==1):
            _emp = NewEmpCategory['FULLTIME']
        elif (age>=3) & (emply==2):
            _emp = NewEmpCategory['PARTTIME']
        elif (age>=3) & (emply>2):
            _emp = NewEmpCategory['UNEMPLOYED']
        elif age<3:
            _emp = NewEmpCategory['NON-WORKER']
        else:
            _emp = NewEmpCategory['UNEMPLOYED']
            self.recode_emp_category(_emp,
                "emp_cat not determined due to unknown status; assign EMP_CAT to value of 3 (unemployed)")

        self.fields['AGE_CAT'] = age
        self.fields['EMPLY'] = emply
        self.fields['EMP_CAT'] = _emp

        return _emp

    def _calc_emp_cat_no_recode(self, age, emply):
        _emp = np.NAN
        if (age>=3) & (emply==1):
            _emp = NewEmpCategory['FULLTIME']
        elif (age>=3) & (emply==2):
            _emp = NewEmpCategory['PARTTIME']
        elif (age>=3) & (emply>2):
            _emp = NewEmpCategory['UNEMPLOYED']
        elif age<3:
            _emp = NewEmpCategory['NON-WORKER']
        else:
            pass

        self.fields['AGE_CAT'] = age
        self.fields['EMPLY'] = emply
        self.fields['EMP_CAT'] = _emp

        return _emp


    def _calc_stu_cat(self, age, stude, schol, emp_cat):
        _stu = np.NAN
        if ((emp_cat == 1) | (schol < 1)):
            if (age < 2):
                _stu = NewStuCategory['SCHOOL']
            else:
                _stu = NewStuCategory['NON-STUDENT']
        elif (schol >= 5):
            if (age < 4):
                _stu = NewStuCategory['SCHOOL']
            else:
                _stu = NewStuCategory['UNIVERSITY']
        else:
            if (age > 4):
                _stu = NewStuCategory['UNIVERSITY']
            else:
                _stu = NewStuCategory['SCHOOL']

        if ( (stude<=0) ):
             _stu = NewStuCategory['NON-STUDENT']


        self.fields['STUDE'] = stude
        self.fields['SCHOL'] = schol
        self.fields['STU_CAT'] = _stu

        return _stu

    def _calc_stu_cat_no_recode(self, age, stude, schol, emp_cat):
        _stu = np.NAN
        if (stude <= 0 | ((emp_cat == 1) | (schol < 1))):
            if (age < 3):
                _stu = NewStuCategory['SCHOOL']
            else:
                _stu = NewStuCategory['NON-STUDENT']
        elif (schol >= 5):
            if (age < 3):
                _stu = NewStuCategory['SCHOOL']
            else:
                _stu = NewStuCategory['UNIVERSITY']
        else:
            if (age > 4):
                _stu = NewStuCategory['UNIVERSITY']
            else:
                _stu = NewStuCategory['SCHOOL']

        self.fields['STUDE'] = stude
        self.fields['SCHOL'] = schol
        self.fields['STU_CAT'] = _stu

        return _stu

    def _calc_per_type(self, df_per):
        _type = np.NAN

        if len(df_per) != 1:
            self.log_error("person record not found")

        _age = df_per['AGE_CAT'].iloc[0]
        _emply = df_per['EMPLY'].iloc[0]
        _stude = df_per['STUDE'].iloc[0]
        _schol = df_per['SCHOL'].iloc[0]
        _emp_cat = self._calc_emp_cat(_age, _emply)
        _stu_cat = self._calc_stu_cat(_age, _stude, _schol, _emp_cat)

        if (_age>=3) & (_emp_cat==NewEmpCategory['FULLTIME']):
            #Full-time worker
            _type = NewPerType['FW']

        elif (_age>=3) & (_emp_cat==NewEmpCategory['PARTTIME']) & (_stu_cat==NewStuCategory['NON-STUDENT']):
            #Part-time worker
            _type = NewPerType['PW']

        elif (_age>=3) & (_emp_cat in [NewEmpCategory['PARTTIME'],NewEmpCategory['UNEMPLOYED']]) & (_stu_cat==NewStuCategory['UNIVERSITY']):
            #University Student
            _type = NewPerType['US']

        elif (_age>=3) & (_age<=6) & (_emp_cat==NewEmpCategory['UNEMPLOYED']) & (_stu_cat==NewStuCategory['NON-STUDENT']):
            #Non-worker
            _type = NewPerType['NW']

        elif (_age>=7) & (_emp_cat==NewEmpCategory['UNEMPLOYED']) & (_stu_cat==NewStuCategory['NON-STUDENT']):
            #Retired (non-working) adult
            _type = NewPerType['RE']

        #elif (_age>=16) & (_age<=19) & (_emp_cat in [2,3]) & (_stu_cat==1):
        # note -- inclusive up to age 24 given categories
        elif (_age>=3) & (_age<=6) & (_stu_cat==NewStuCategory['SCHOOL']):
            #Driving Age Student
            _type = NewPerType['DS']

        elif (_age==2) & (_emp_cat==NewEmpCategory['NON-WORKER']) & (_stu_cat==NewStuCategory['SCHOOL']):
            #Non-driving student
            _type = NewPerType['ND']

        elif (_age==2) & (_emp_cat==NewEmpCategory['NON-WORKER']) & (_stu_cat==NewStuCategory['NON-STUDENT']):    #recode none students as students; see email exchange with JF on 6/5/2014
            #Non-driving student
            _type = NewPerType['ND']
            self.recode_student_category(NewStuCategory['SCHOOL'], "6~15 year old not attending school; reset STU_CAT to SCHOOL and assign PERTYPE to ND")

        elif (_age==1): 
            #Preschool
            _type = NewPerType['PS']

        else:
            self.log_error("per_type not found")

        return _type

    def _calc_per_type_no_recode(self, df_per):
        _type = np.NAN

        if len(df_per) != 1:
            self.log_error("person record not found")

        _age = df_per['AGE_CAT'].iloc[0]
        _emply = df_per['EMPLY'].iloc[0]
        _stude = df_per['STUDE'].iloc[0]
        _schol = df_per['SCHOL'].iloc[0]
        _emp_cat = self._calc_emp_cat(_age, _emply)
        _stu_cat = self._calc_stu_cat(_age, _stude, _schol, _emp_cat)

        if (_age>=3) & (_emp_cat==NewEmpCategory['FULLTIME']):
            #Full-time worker
            _type = NewPerType['FW']

        elif (_age>=3) & (_emp_cat==NewEmpCategory['PARTTIME']) & (_stu_cat==NewStuCategory['NON-STUDENT']):
            #Part-time worker
            _type = NewPerType['PW']

        elif (_age>=3) & (_emp_cat in [NewEmpCategory['PARTTIME'],NewEmpCategory['UNEMPLOYED']]) & (_stu_cat==NewStuCategory['UNIVERSITY']):
            #University Student
            _type = NewPerType['US']

        elif (_age>=3) & (_age<=6) & (_emp_cat==NewEmpCategory['UNEMPLOYED']) & (_stu_cat==NewStuCategory['NON-STUDENT']):
            #Non-worker
            _type = NewPerType['NW']

        elif (_age>=7) & (_emp_cat==NewEmpCategory['UNEMPLOYED']) & (_stu_cat==NewStuCategory['NON-STUDENT']):
            #Retired (non-working) adult
            _type = NewPerType['RE']

        elif (_age>=3) & (_age<=17) & (_stu_cat==NewStuCategory['SCHOOL']):
            #Driving Age Student
            _type = NewPerType['DS']

        elif (_age==3) & (_stu_cat==NewStuCategory['UNIVERSITY']):    #recode 16-year olds going to university as going to school
            #Driving Age Student
            _type = NewPerType['DS']

        elif (_age==2) & (_emp_cat==NewEmpCategory['NON-WORKER']) & (_stu_cat==NewStuCategory['SCHOOL']):
            #Non-driving student
            _type = NewPerType['ND']

        elif (_age==2) & (_emp_cat==NewEmpCategory['NON-WORKER']) & (_stu_cat==NewStuCategory['NON-STUDENT']):    #recode none students as students; see email exchange with JF on 6/5/2014
            #Non-driving student
            _type = NewPerType['ND']

        elif (_age==1): #see email exchange with Joel 6/5/2014
            #Preschool
            _type = NewPerType['PS']

        else:
            self.log_error("per_type not found")

        return _type


    def add_tour(self, tour):
        self.tours.append(tour)

    def get_id(self):
        return self.per_id

    def get_is_adult(self):
        ptype = self.get_per_type()
        return ptype in [NewPerType['FW'], NewPerType['PW'],
                                 NewPerType['US'], NewPerType['NW'],
                                 NewPerType['RE'] ]

    def set_per_type(self, ptype):
        self.fields['PERSONTYPE'] = ptype

    def get_per_type(self):
        if 'PERSONTYPE' in self.fields:
            return self.fields['PERSONTYPE']
        else:
            return np.NAN

    def recode_per_type(self, ptype, msg):
        prev_value = self.get_per_type()
        #update field to new value
        self.fields['PERSONTYPE'] = ptype
        #save old value in a different field
        self.fields['PERSONTYPE0'] = prev_value
        #save edits to the log
        self.log_recode(msg)

    def set_student_category(self, stu_cat):
        self.fields['STU_CAT'] = stu_cat

    def get_student_category(self):
        if 'STU_CAT' in self.fields:
            return self.fields['STU_CAT']
        else:
            return np.NAN

    def recode_student_category(self, stu_cat, msg):
        prev_value = self.get_student_category()
        #update field to new value
        self.fields['STU_CAT'] = stu_cat
        #save old value in a different field
        self.fields['STU_CAT0'] = prev_value
        #save edits to the log
        self.log_recode(msg)

    def set_emp_category(self, emp_cat):
        self.fields['EMP_CAT'] = emp_cat

    def get_emp_category(self):
        if 'EMP_CAT' in self.fields:
            return self.fields['EMP_CAT']
        else:
            return np.NAN

    def recode_emp_category(self, emp_cat, msg):
        prev_value = self.get_emp_category()
        #update field to new value
        self.fields['EMP_CAT'] = emp_cat
        #save old value in a different field
        self.fields['EMP_CAT0'] = prev_value
        #save edits to the log
        self.log_recode(msg)


    def add_subtour(self, trips):
        """ add an at-work subtour to its list of tours; return the subtour obj """
        #id of the new tour is 1 up the id of the last tour currently in the list
        _new_tour_id = 1 + self.tours[-1].get_id()
        #create tour object
        #_new_tour = Tour(self.hh_obj.get_id(), self.per_id, _new_tour_id, 1)
        _new_tour = Tour(self.hh_obj, self, _new_tour_id, 1, trips)
        #add new tour to its list
        self.tours.append(_new_tour)

        return _new_tour

    def lastTripEndAtHome(self):
        _returned_home = True
        _last_trip = self.tours[-1].trips[-1]
        if _last_trip.fields['DEST_PURP'] != NewPurp['HOME']:
            _returned_home = False
        return _returned_home

    def log_recode(self, msg):
        self.hh_obj.log_recode("PER_ID={} \t".format(self.per_id)+msg)

    def log_warning(self, msg):
        self.hh_obj.log_warning("\t <Warning> Person#{}: ".format(self.per_id)+msg)

    def log_error(self, err_msg=None):
        self.error_flag = True
        self.fields['ERROR'] = "E:"
        if err_msg:
            self.hh_obj.log_error("\t Person#{}: ".format(self.per_id)+err_msg)
            self.fields['ERROR'] = self.fields['ERROR']+err_msg

class Joint_tour:
    """ Joint tour class """
    #upon creation of a joint tour, determine the joint tour purpose and pass it on to the constituting person tours
    def __init__(self, jtour_id, jtrips):
        self.jtour_id = jtour_id
        self.jtrips = jtrips    #list of Joint_trip objects
        self.error_msg = ""
        self.error_flag = False
        #find all the person tours associated with this fully joint tour
        self.person_tours = []
        for jt in jtrips[0]:
            ptrip = jt.parent_trip
            ptour = ptrip.tour_obj
            self.person_tours.append(ptour)
        self.joint_purp = self.find_joint_purp()
        for ptour in self.person_tours:
            ptour.set_joint_tour_purp(self.joint_purp)

    def find_joint_purp(self):
        """given the person tour associated with this fully joint tour, return the joint tour purpose """
        # this is intended to prevent joint tour purpose being coded as escorting.
        purp_list= []
        joint_purp = None
        for tour in self.person_tours:
            purp = tour.get_purp()
            if (not purp==NewPurp['ESCORTING']) & (not purp in purp_list):
                purp_list.append(purp)
        #in theory, there should be only 1 purpose
        if len(purp_list)==0:
            joint_purp = NewPurp['ESCORTING']
            #self.log_error("No valid joint purpose found")
        elif len(purp_list)>1:
            #self.log_error("More than 1 joint purposes found: "+','.join(['%s' %purp for purp in purp_list]))
            #determine based on purpose hierarchy
            joint_purp = min(purp_list)
        else:
            #there is exactly one (non-escorting) purpose found across all participants
            joint_purp = purp_list[0]
        return joint_purp

    def log_error(self, err_msg=None):
        self.error_flag = True
        if err_msg:     #if there is an error message
            self.error_msg = self.error_msg + "E: " + err_msg

    def print_header(fp):
        _header=["HH_ID", "JTOUR_ID", "NUMBER_HH"]
        #PERSON_1 to PERSON_9
        for _i in range(1, 10):
            _header.append("PERSON_"+str(_i))
        _header.extend(["COMPOSITION", "JOINT_PURP"])
        fp.write(','.join(['%s' %field for field in _header])+'\n')
        #Trip.print_header(fp)

    def print_vals(self, fp):
        #write out hh id, joint tour id, and size of travel group
        #use the 0'th joint trip in the 0'th joint trip group since all joint trips in the jtrips list contain the same information
        _jt = self.jtrips[0][0]
        fp.write('{},{},{}'.format(_jt.get_hh_id(), _jt.jtour_id, _jt.number_hh))
        #write out IDs of people in the travel party
        _party = sorted(_jt.travel_party)
        for _i in range(0, _jt.number_hh):
            fp.write(',{}'.format(_party[_i]))
        #fill nan up to person 9
        for _i in range(_jt.number_hh, 9):
            fp.write(',nan')
        #write out group composition, joint purpose, and error
        fp.write(',{},{}\n'.format(_jt.composition, self.joint_purp))

        #_jt.parent_trip.print_vals(fp)


class Tour:
    """ Tour class """
    def __init__(self, hh_obj, per_obj, tour_id, is_subtour=0, trips=None):
        self.hh_obj = hh_obj
        self.per_obj = per_obj
        self.tour_id = int(tour_id)
        self.is_AW_subtour = is_subtour
        self.trips = []
        if trips!=None:
            #this is a AW_subtour
            #re-set trips' tour_obj to the current tour & reset ID field
            for _trip in trips:
                _trip.tour_obj = self
                _trip.fields['TOUR_ID'] = self.tour_id
            #add trips to this tour's list of trips
            self.trips.extend(trips)
            #renumber trips is needed when tour is created for a subtour
            self.renumber_trips()

        self.fields = {'HH_ID': hh_obj.get_id(), 'PER_ID': per_obj.get_id(), 'TOUR_ID': self.tour_id, 'IS_SUBTOUR':self.is_AW_subtour, 'FULLY_JOINT':0}
        self.set_partial(NewPartialTour['NOT_PARTIAL'])     #set as default value
        self.error_flag = False

    def add_trip(self, trip):
        self.trips.append(trip)

    def add_trips(self, trip_list):
        #add the new trips from trip_list to the end of the existing list of trips
        self.trips.extend(trip_list)

    def renumber_trips(self):
        """ renumber the trips in the trip list based on their physical order in the list """
        for _i, _trip in enumerate(self.trips):    #trip index starts with 0
            _trip.set_id(_i+1)          #trip id starts with 1

    def get_id(self):
        return self.tour_id

    def get_trip(self, trip_id):
        #trips in the list are sequentially numbered from 1
        return self.trips[trip_id-1]

    def get_mode(self):
        if 'TOURMODE' in self.fields:   #make sure the key exists
            return self.fields['TOURMODE']

    def get_purp(self):
        if 'TOURPURP' in self.fields:   #make sure the key exists
            return self.fields['TOURPURP']

    def contains_joint_trip(self, jtrip_id):
        for _trip in self.trips:
            if _trip.get_jtripID() == jtrip_id:
                val = True
                break
            else:
                val = False
        return val

    def get_is_escorting(self):
        escorting = 0
        for _trip in self.trips:
            if _trip.get_is_escorting():
                escorting = 1
                break
        return escorting

    def get_partial_status(self):
        if 'PARTIAL_TOUR' in self.fields:   #make sure the key exists
            return self.fields['PARTIAL_TOUR']
        else:
            return NewPartialTour['NOT_PARTIAL']

    def get_is_subtour(self):
        _is_subtour = 0
        if 'IS_SUBTOUR' in self.fields:   #make sure the key exists
            if self.fields['IS_SUBTOUR']==1:
                _is_subtour=1
        return _is_subtour


    def get_outbound_stops(self):
        if 'OUTBOUND_STOPS' in self.fields:   #make sure the key exists
            return self.fields['OUTBOUND_STOPS']
        else:
            return None                         #TODO: this would be error

    def get_inbound_stops(self):
        if 'INBOUND_STOPS' in self.fields:   #make sure the key exists
            return self.fields['INBOUND_STOPS']
        else:
            return None                         #TODO: this would be error

    def get_num_trips(self):
        return len(self.trips)

    def get_is_fully_joint(self):
        fully_joint = 0
        if 'FULLY_JOINT' in self.fields:   #make sure the key exists
            if self.fields['FULLY_JOINT'] == 1:
                fully_joint = 1
        return fully_joint

    def log_error(self, err_msg=None):
        self.error_flag = True
        self.fields['ERROR'] = "E: "
        if err_msg:
            self.hh_obj.log_error("\t Person#{} Tour#{}: ".format(self.per_obj.get_id(), self.tour_id)+err_msg)
            self.fields['ERROR'] = self.fields['ERROR']+err_msg

    def _calc_tour_mode(self):
        _mode = -1
        _transit_mode = ''
        _access_mode = ''
        #put the modes used on all trips in one list
        _modes_used = set()
        for _trip in self.trips:
            _modes_used.add(_trip.fields['TRIPMODE'])
        
        # get driver
        _is_driver = int(0)
        for _trip in self.trips:
             if _trip.fields['ISDRIVER']>0:
                 _is_driver = 1

        #determine the tour mode
        # Code transit mode

        if NewTripMode['KNR-TRANSIT'] in _modes_used or NewTripMode['PNR-TRANSIT'] in _modes_used or NewTripMode['WALK-TRANSIT'] or NewTripMode['TNC-TRANSIT'] in _modes_used:
            _transit_mode = 'TRANSIT'
        else:
            _transit_mode = ''

        if (_transit_mode == 'TRANSIT') & (NewTripMode['PNR-TRANSIT'] in _modes_used) :
            _access_mode = 'PNR'
        elif (_transit_mode == 'TRANSIT') & ((NewTripMode['KNR-TRANSIT'] in _modes_used)):
            _access_mode = 'KNR'
        elif (_transit_mode == 'TRANSIT') & ((NewTripMode['TNC-TRANSIT'] in _modes_used)):
            _access_mode = 'TNR'
        elif (NewTripMode['WALK-TRANSIT']) in _modes_used:
            _access_mode = 'WALK'
        else:
            _access_mode = ''

        if NewTripMode['SCHOOLBUS'] in _modes_used:
            _mode = NewTourMode['SCHOOLBUS']
        elif _transit_mode == 'TRANSIT' and _access_mode == 'PNR':
            _mode = NewTourMode['PNR-TRANSIT']  
        elif _transit_mode == 'TRANSIT' and _access_mode == 'KNR':
            _mode = NewTourMode['KNR-TRANSIT']   
        elif _transit_mode == 'TRANSIT' and _access_mode == 'TNR':
            _mode = NewTourMode['TNC-TRANSIT']  
        elif _transit_mode == 'TRANSIT' and _access_mode == 'WALK':
            _mode = NewTourMode['WALK-TRANSIT']   
        elif NewTripMode['TNC-POOL'] in _modes_used:
            _mode = NewTourMode['TNC-POOL']
        elif NewTripMode['TNC-REG'] in _modes_used:
            _mode = NewTourMode['TNC-REG']
        elif NewTripMode['TAXI'] in _modes_used:
            _mode = NewTourMode['TAXI']        
        elif NewTripMode['BIKE'] in _modes_used:
            _mode = NewTourMode['BIKE']        
        elif NewTripMode['HOV3'] in _modes_used:
            _mode = NewTourMode['HOV3']  
        elif NewTripMode['HOV2'] in _modes_used:
            _mode = NewTourMode['HOV2']  
        elif NewTripMode['SOV'] in _modes_used:
            _mode = NewTourMode['SOV']  
        elif NewTripMode['WALK'] in _modes_used:
            _mode = NewTourMode['WALK']  
        else:
            _mode = NewTourMode['OTHER']

        return _mode
        print(_mode)


    def _get_is_driver(self):
        _is_driver = int(0)
        for _trip in self.trips:
           # if 'DRIVER' in _trip.fields:      #make sure that the key exists before looking up its value
           if _trip.fields['ISDRIVER']>0:
                _is_driver = 1
            #        break
        return _is_driver

    def _set_outbound_stops(self, first_ob_trip, last_ob_trip):
        _num_ob_stops = last_ob_trip - first_ob_trip
        self.fields['OUTBOUND_STOPS']  = _num_ob_stops
        for _j in range(0,_num_ob_stops):
            #set field values for each outbound stop
            _label = 'OSTOP_'+str(_j+1)+'_'
            self.fields[_label+'PLACENO']  = self.trips[_j].fields['DEST_PLACENO']
            self.fields[_label+'X']        = self.trips[_j].fields['DEST_X']
            self.fields[_label+'Y']        = self.trips[_j].fields['DEST_Y']
            self.fields[_label+'ARR_HR']   = self.trips[_j].fields['DEST_ARR_HR']
            self.fields[_label+'ARR_MIN']  = self.trips[_j].fields['DEST_ARR_MIN']
            self.fields[_label+'ARR_BIN']  = self.trips[_j].fields['DEST_ARR_BIN']
            self.fields[_label+'DEP_HR']   = self.trips[_j].fields['DEST_DEP_HR']
            self.fields[_label+'DEP_MIN']  = self.trips[_j].fields['DEST_DEP_MIN']
            self.fields[_label+'DEP_BIN']  = self.trips[_j].fields['DEST_DEP_BIN']

            #calculate stop duration based on arrival and departure at stop
            (self.fields[_label+'DUR_HR'], self.fields[_label+'DUR_MIN']) = calculate_duration(
                                                                                        self.fields[_label+'ARR_HR'], self.fields[_label+'ARR_MIN'],
                                                                                        self.fields[_label+'DEP_HR'], self.fields[_label+'DEP_MIN'])
            self.fields[_label+'DUR_BIN'] = 1 + self.fields[_label+'DEP_BIN'] - self.fields[_label+'ARR_BIN']

            self.fields[_label+'PURP']  = self.trips[_j].fields['DEST_PURP']
            self.fields[_label+'MODE']  = self.trips[_j].fields['TRIPMODE']
            self.fields[_label+'PUDO']  = self.trips[_j].fields['DEST_ESCORTING']

            #set the stop number in the trip record too
            self.trips[_j].set_stop_number_on_half_tour(_j+1)

        #if _num_ob_stops>4:
        #    self.log_error("found {} outbound stops".format(_num_ob_stops))


    def _set_inbound_stops(self, first_ib_trip, last_ib_trip):
        _num_ib_stops = int(last_ib_trip - first_ib_trip)
        self.fields['INBOUND_STOPS']  = _num_ib_stops
        for _j in range(first_ib_trip, last_ib_trip):
            #set field values for each inbound stop
            _label = 'ISTOP_'+str(_j-first_ib_trip+1)+'_'
            self.fields[_label+'PLACENO']  = self.trips[_j].fields['DEST_PLACENO']
            self.fields[_label+'X']        = self.trips[_j].fields['DEST_X']
            self.fields[_label+'Y']        = self.trips[_j].fields['DEST_Y']
            self.fields[_label+'ARR_HR']   = self.trips[_j].fields['DEST_ARR_HR']
            self.fields[_label+'ARR_MIN']  = self.trips[_j].fields['DEST_ARR_MIN']
            self.fields[_label+'ARR_BIN']  = self.trips[_j].fields['DEST_ARR_BIN']
            self.fields[_label+'DEP_HR']   = self.trips[_j].fields['DEST_DEP_HR']
            self.fields[_label+'DEP_MIN']  = self.trips[_j].fields['DEST_DEP_MIN']
            self.fields[_label+'DEP_BIN']  = self.trips[_j].fields['DEST_DEP_BIN']
            #calculate stop duration based on arrival and departure at stop
            (self.fields[_label+'DUR_HR'], self.fields[_label+'DUR_MIN']) = calculate_duration(
                                                                                        self.fields[_label+'ARR_HR'], self.fields[_label+'ARR_MIN'],
                                                                                        self.fields[_label+'DEP_HR'], self.fields[_label+'DEP_MIN'])
            self.fields[_label+'DUR_BIN'] = 1 + self.fields[_label+'DEP_BIN'] - self.fields[_label+'ARR_BIN']

            self.fields[_label+'PURP']  = self.trips[_j].fields['DEST_PURP']
            self.fields[_label+'MODE']  = self.trips[_j].fields['TRIPMODE']
            self.fields[_label+'PUDO']  = self.trips[_j].fields['DEST_ESCORTING']

            #set the stop number in the trip record too
            self.trips[_j].set_stop_number_on_half_tour(_j-first_ib_trip+1)

        #if _num_ib_stops>4:
        #    self.log_error("found {} inbound stops".format(_num_ib_stops))

    def populate_attributes(self):
        """ determine tour attributes based on its constituting trips """

        if self.get_partial_status()==NewPartialTour['PARTIAL_START']:
            #assume primary destination is the origin of first trip of the day
            _prim_purp = self.trips[0].get_orig_purpose()
            self.fields['TOURPURP'] = _prim_purp

            #tour mode
            self.fields['TOURMODE'] = self._calc_tour_mode()

            #is person a driver on any trip
            self.fields['DRIVER'] = self._get_is_driver()

            #tour destination: given by trips[_prim_i]
            self.fields['DEST_PLACENO']         = self.trips[0].fields['ORIG_PLACENO']
            self.fields['DEST_X']               = self.trips[0].fields['ORIG_X']
            self.fields['DEST_Y']               = self.trips[0].fields['ORIG_Y']
            #self.fields['DEST_MODE']            = self.trips[0].fields['TRIPMODE']

            #self.fields['PRIMDEST_ARRIVE_HOUR'] = self.trips[_prim_i].fields['DEST_ARR_HR']
            #self.fields['PRIMDEST_ARRIVE_MIN']  = self.trips[_prim_i].fields['DEST_ARR_MIN']
            self.fields['PRIMDEST_DEPART_HOUR'] = self.trips[0].fields['ORIG_DEP_HR']
            self.fields['PRIMDEST_DEPART_MIN']  = self.trips[0].fields['ORIG_DEP_MIN']
            self.fields['PRIMDEST_DEPART_BIN']  = self.trips[0].fields['ORIG_DEP_BIN']

            #last trip back at tour anchor
            _last_i = len(self.trips)-1              #trips[_last_i] is the last trip on tour
            self.fields['ANCHOR_ARRIVE_HOUR']   = self.trips[_last_i].fields['DEST_ARR_HR']
            self.fields['ANCHOR_ARRIVE_MIN']    = self.trips[_last_i].fields['DEST_ARR_MIN']
            self.fields['ANCHOR_ARRIVE_BIN']    = self.trips[_last_i].fields['DEST_ARR_BIN']

            self.fields['ORIG_MODE']            = self.trips[_last_i].fields['TRIPMODE']

            # DH [02/27/2020] Added TAZ information available in preprocessing
            self.fields['ORIG_TAZ'] = self.trips[_last_i].fields['ORIG_TAZ']
            self.fields['DEST_TAZ'] = self.trips[0].fields['DEST_TAZ']

            #cannot calculate tour duration based on departure from anchor and arrival at anchor
            #no outbound stops
            self._set_outbound_stops(0, 0)

            #inbound stops
            self._set_inbound_stops(0, _last_i)  #inbound stops are dest of trips[0] to trips[_last_i-1]

        elif self.get_partial_status()==NewPartialTour['PARTIAL_END']:
            _prim_purp = self.trips[-1].get_dest_purpose()   #set to purpose at destination of last trip of the day
            self.fields['TOURPURP'] = _prim_purp

            #tour mode
            self.fields['TOURMODE'] = self._calc_tour_mode()

            #is person a driver on any trip
            self.fields['DRIVER'] = self._get_is_driver()

            #tour origin: origin of 1st trip on tour
            self.fields['ORIG_PLACENO']         = self.trips[0].fields['ORIG_PLACENO']
            self.fields['ORIG_X']               = self.trips[0].fields['ORIG_X']
            self.fields['ORIG_Y']               = self.trips[0].fields['ORIG_Y']
            self.fields['ANCHOR_DEPART_HOUR']   = self.trips[0].fields['ORIG_DEP_HR']
            self.fields['ANCHOR_DEPART_MIN']    = self.trips[0].fields['ORIG_DEP_MIN']
            self.fields['ANCHOR_DEPART_BIN']    = self.trips[0].fields['ORIG_DEP_BIN']

            #tour destination: given by trips[-1] aka last trip
            self.fields['DEST_PLACENO']         = self.trips[-1].fields['DEST_PLACENO']
            self.fields['DEST_X']               = self.trips[-1].fields['DEST_X']
            self.fields['DEST_Y']               = self.trips[-1].fields['DEST_Y']
            self.fields['DEST_MODE']            = self.trips[-1].fields['TRIPMODE']

            # DH [02/27/2020] Added TAZ information available in preprocessing
            self.fields['ORIG_TAZ'] = self.trips[0].fields['ORIG_TAZ']
            self.fields['DEST_TAZ'] = self.trips[-1].fields['DEST_TAZ']

            self.fields['PRIMDEST_ARRIVE_HOUR'] = self.trips[-1].fields['DEST_ARR_HR']
            self.fields['PRIMDEST_ARRIVE_MIN']  = self.trips[-1].fields['DEST_ARR_MIN']
            self.fields['PRIMDEST_ARRIVE_BIN']  = self.trips[-1].fields['DEST_ARR_BIN']
            #self.fields['PRIMDEST_DEPART_HOUR'] = self.trips[_prim_i].fields['DEST_DEP_HR']
            #self.fields['PRIMDEST_DEPART_MIN']  = self.trips[_prim_i].fields['DEST_DEP_MIN']

            #last trip back at tour anchor unknown
            #cannot calculate tour duration based on departure from anchor and arrival at anchor

            #outbound stops
            self._set_outbound_stops(0, len(self.trips)-1)         #outbound stops are dest of trips[0] to trips[_prim_i-1]

            #no inbound stops
            self._set_inbound_stops(len(self.trips)-1, len(self.trips)-1)


        else:
            _prim_i = self._find_prim_stop()     #destination of trips[_prim_i] is the primary destination

            #determine tour purpose (at primary destination)
            _prim_purp = self.trips[_prim_i].fields['DEST_PURP']
            self.fields['TOURPURP'] = _prim_purp

            #extract at-work subtours from the current tour if the current tour is a WORK tour
            #this needs to be done before processing any outbound and inbound stops
            if _prim_purp==NewPurp['WORK']:
                self._set_AW_subtours(_prim_i)
                #TODO: trip id may have been changed when subtours are created
                #quick-fix: re-calculate primary destination
                _prim_i = self._find_prim_stop()     #destination of trips[_prim_i] is the primary destination
                _prim_purp = self.trips[_prim_i].fields['DEST_PURP']
                self.fields['TOURPURP'] = _prim_purp

            #tour mode
            self.fields['TOURMODE'] = self._calc_tour_mode()

            #is person a driver on any trip
            self.fields['DRIVER'] = self._get_is_driver()

            #tour origin: origin of 1st trip on tour
            self.fields['ORIG_PLACENO']         = self.trips[0].fields['ORIG_PLACENO']
            self.fields['ORIG_X']               = self.trips[0].fields['ORIG_X']
            self.fields['ORIG_Y']               = self.trips[0].fields['ORIG_Y']
            self.fields['ANCHOR_DEPART_HOUR']   = self.trips[0].fields['ORIG_DEP_HR']
            self.fields['ANCHOR_DEPART_MIN']    = self.trips[0].fields['ORIG_DEP_MIN']
            self.fields['ANCHOR_DEPART_BIN']    = self.trips[0].fields['ORIG_DEP_BIN']

            # DH [02/21/2020] Added TAZ information available in preprocessing
            self.fields['ORIG_TAZ'] = self.trips[0].fields['ORIG_TAZ']
            self.fields['DEST_TAZ'] = self.trips[_prim_i].fields['DEST_TAZ']

            #tour destination: given by trips[_prim_i]
            self.fields['DEST_PLACENO']         = self.trips[_prim_i].fields['DEST_PLACENO']
            self.fields['DEST_X']               = self.trips[_prim_i].fields['DEST_X']
            self.fields['DEST_Y']               = self.trips[_prim_i].fields['DEST_Y']
            self.fields['DEST_MODE']            = self.trips[_prim_i].fields['TRIPMODE']

            self.fields['PRIMDEST_ARRIVE_HOUR'] = self.trips[_prim_i].fields['DEST_ARR_HR']
            self.fields['PRIMDEST_ARRIVE_MIN']  = self.trips[_prim_i].fields['DEST_ARR_MIN']
            self.fields['PRIMDEST_ARRIVE_BIN']  = self.trips[_prim_i].fields['DEST_ARR_BIN']

            self.fields['PRIMDEST_DEPART_HOUR'] = self.trips[_prim_i].fields['DEST_DEP_HR']
            self.fields['PRIMDEST_DEPART_MIN']  = self.trips[_prim_i].fields['DEST_DEP_MIN']
            self.fields['PRIMDEST_DEPART_BIN']  = self.trips[_prim_i].fields['DEST_DEP_BIN']

            #last trip back at tour anchor
            _last_i = len(self.trips)-1              #trips[_last_i] is the last trip on tour
            self.fields['ANCHOR_ARRIVE_HOUR']   = self.trips[_last_i].fields['DEST_ARR_HR']
            self.fields['ANCHOR_ARRIVE_MIN']    = self.trips[_last_i].fields['DEST_ARR_MIN']
            self.fields['ANCHOR_ARRIVE_BIN']    = self.trips[_last_i].fields['DEST_ARR_BIN']
            self.fields['ORIG_MODE']            = self.trips[_last_i].fields['TRIPMODE']

            #calculate tour duration based on departure from anchor and arrival at anchor
            (self.fields['TOUR_DUR_HR'], self.fields['TOUR_DUR_MIN']) = calculate_duration(self.fields['ANCHOR_DEPART_HOUR'], self.fields['ANCHOR_DEPART_MIN'],
                                                                                           self.fields['ANCHOR_ARRIVE_HOUR'], self.fields['ANCHOR_ARRIVE_MIN'])
            self.fields['TOUR_DUR_BIN'] = 1 + self.fields['ANCHOR_ARRIVE_BIN'] - self.fields['ANCHOR_DEPART_BIN']

            if len(self.trips)==1:  #loop tour
                #check if purpose is 'loop
                if not self.fields['TOURPURP']==NewPurp['LOOP']:
                    self.log_error("Only 1 trip in the tour, but purpose is {}".format(self.fields['TOURPURP']))
                #outbound stops
                self._set_outbound_stops(0, 0)
                #inbound stops
                self._set_inbound_stops(0,0)
            else:
                #outbound stops
                self._set_outbound_stops(0, _prim_i)         #outbound stops are dest of trips[0] to trips[_prim_i-1]
                #inbound stops
                self._set_inbound_stops(_prim_i+1, _last_i)  #inbound stops are dest of trips[_prim_i+1] to trips[_last_i-1]

        #calculate tour distance
        if COMPUTE_TRIP_DIST:
            _dist = 0
            _missing_dist = False
            for _trip in self.trips:
                if _trip.get_dist() >= 0:
                    _dist = _dist + _trip.get_dist()
                else:
                    _missing_dist = True
                    self.log_error("cannot compute tour distance")
                    break
            if _missing_dist:
                self.fields['DIST'] = np.NAN
            else:
                self.fields['DIST'] = _dist

    def set_partial(self, is_partial):
        self.fields['PARTIAL_TOUR'] = is_partial


    def set_fully_joint(self, jtour_id):
        self.fields['FULLY_JOINT'] = 1  #initialize field to 0 (otherwise will be written out as 'nan')
        self.fields['JTOUR_ID'] = jtour_id

    def set_parent(self, parent_tour):
        """ set field values relating to the parent tour """
        self.fields['PARENT_TOUR_ID']   = parent_tour.get_id()
        self.fields['PARENT_TOUR_MODE'] = parent_tour.get_mode()

    def set_per_type(self, per_type):
        self.fields['PERSONTYPE'] = per_type

    def set_joint_tour_purp(self, purp):
        self.fields['JOINT_TOUR_PURP'] = purp

    def set_joint_status(self):

        #classify a tour as one of the following:
        # (1) independent tours:        no trips are joint
        # (2) fully-joint tours:        tour.get_is_fully_joint()==True (all trips are joint and made by the same group of people)
        # (3) partially-joint tours:    at least 1 joint trip; all joint trips are properly grouped; but not fully joint
        # (4) joint, but problematic tours :    at least 1 problematic joint trip
        num_trips = self.get_num_trips()
        num_grouped_joint_trips = self.get_num_grouped_joint_trips()
        num_problem_joint_trips = self.get_num_prob_joint_trips()
        num_indep_trips = num_trips - num_grouped_joint_trips - num_problem_joint_trips

        if num_trips==num_indep_trips:
            status = NewJointTourStatus['INDEPENDENT']
        elif self.get_is_fully_joint():
            status = NewJointTourStatus['FULL_JOINT']
        elif num_grouped_joint_trips>0 & num_problem_joint_trips==0:
            status = NewJointTourStatus['PART_JOINT']
        else:
            status = NewJointTourStatus['PROB_JOINT']

        self.fields['JOINT_STATUS'] = status

    def get_num_grouped_joint_trips(self):
        count = 0
        for trip in self.trips:
            if trip.get_joint_status()==NewJointCategory['JOINT-GROUPED']:
                count = count+1
        return count

    def get_num_prob_joint_trips(self):
        count = 0
        for trip in self.trips:
            if trip.get_joint_status()==NewJointCategory['JOINT']:
                count = count+1
        return count

    def set_escortee_purp(self, jtrip_id, escortee_purp):
        for _trip in self.trips:
            if _trip.get_jtripID() == jtrip_id:
                if _trip.fields['IS_INBOUND']==0:
                    self.fields['OUT_ESCORTEE_TOUR_PURP'] = escortee_purp
                else:
                    self.fields['INB_ESCORTEE_TOUR_PURP'] = escortee_purp


    def set_escorted_fields(self):
        _escorted = False
        _chauffuer_set = set()
        _out_escort_type = np.NAN
        _inb_escort_type = np.NAN
        _out_chauffuer = np.NAN
        _inb_chauffuer = np.NAN
        _out_chauffer_purp = np.NAN
        _inb_chauffer_purp = np.NAN
        _out_chauffer_ptype = np.NAN
        _inb_chauffer_ptype = np.NAN
        _household = self.hh_obj
        _purpose = self.get_purp()
        for _trip in self.trips:
            #set union with the trip's chauffeur, if applicable
            if _trip.get_escorted()!=NewEscort['NEITHER']:
                _escorted = True
                _chauffuer_set.add(_trip.get_chauffuer())
            #find chauffeur for outbound leg of the tour
            if (_trip.fields['IS_INBOUND']==0) & (_trip.get_escorted()!=NewEscort['NEITHER']):
                _out_chauffuer = _trip.get_chauffuer()
                _out_jtripID = _trip.get_jtripID()
                #find tour purpose for chauffeur for outbound jtripID
                _out_chauffuer_per_Obj = _household.get_person(_out_chauffuer)
                if _out_chauffuer_per_Obj is None:
                    _out_chauffer_ptype = 0
                else:
                    _out_chauffer_ptype = _out_chauffuer_per_Obj.fields['PERSONTYPE']
                    for _ctour in _out_chauffuer_per_Obj.tours:
                        if _ctour.contains_joint_trip(_out_jtripID) == True:
                            _out_chauffer_purp = _ctour.get_purp()
                            _ctour.set_escortee_purp(_out_jtripID, _purpose)
            #find chauffeur for inbound leg of the tour
            if (_trip.fields['IS_INBOUND']==1) & (_trip.get_escorted()!=NewEscort['NEITHER']):
                _inb_chauffuer = _trip.get_chauffuer()
                _inb_jtripID = _trip.get_jtripID()
                #find tour purpose for chauffeur for inbound jtripID
                _inb_chauffuer_per_Obj = _household.get_person(_inb_chauffuer)
                if _inb_chauffuer_per_Obj is None:
                    _inb_chauffer_ptype = 0
                else:
                    _inb_chauffer_ptype = _inb_chauffuer_per_Obj.fields['PERSONTYPE']
                    for _ctour in _inb_chauffuer_per_Obj.tours:
                        if _ctour.contains_joint_trip(_inb_jtripID) == True:
                            _inb_chauffer_purp = _ctour.get_purp()
                            _ctour.set_escortee_purp(_inb_jtripID, _purpose)
        #code escort type on outbound and inbound leg of the tour
        if  not math.isnan(_out_chauffer_purp):
            if ((_out_chauffer_purp>0) & (_out_chauffer_purp<=3)) | (_out_chauffer_purp==10):
                _out_escort_type = NewEscortType['RIDE_SHARING']
            elif (_out_chauffer_purp>3) & (_out_chauffer_purp<10):
                _out_escort_type = NewEscortType['PURE_ESCORT']
        elif (_out_chauffer_ptype==4) | (_out_chauffer_ptype==5):
            _out_escort_type = NewEscortType['PURE_ESCORT']
        elif ((_out_chauffer_ptype > 0) & (_out_chauffer_ptype <=3)) | (_out_chauffer_ptype==6):
            _out_escort_type = NewEscortType['RIDE_SHARING']
        else:
            _out_escort_type = NewEscortType['NO_ESCORT']

        if not math.isnan(_inb_chauffer_purp):
            if ((_inb_chauffer_purp>0) & (_inb_chauffer_purp<=3)) | (_inb_chauffer_purp==10):
                _inb_escort_type = NewEscortType['RIDE_SHARING']
            elif (_inb_chauffer_purp>3) & (_inb_chauffer_purp<10):
                _inb_escort_type = NewEscortType['PURE_ESCORT']
        elif (_inb_chauffer_ptype==4) | (_inb_chauffer_ptype==5):
            _inb_escort_type = NewEscortType['PURE_ESCORT']
        elif ((_inb_chauffer_ptype > 0) & (_inb_chauffer_ptype <=3)) | (_inb_chauffer_ptype==6):
            _inb_escort_type = NewEscortType['RIDE_SHARING']
        else:
            _inb_escort_type = NewEscortType['NO_ESCORT']

        self.fields['ESCORTED_TOUR'] = int(_escorted)
        self.fields['CHAUFFUER_ID'] = '"'+','.join(['%s' %id for id in list(_chauffuer_set)])+'"'   #in case there are more than 1 chauffuer
        self.fields['OUT_ESCORT_TYPE'] = _out_escort_type
        self.fields['OUT_CHAUFFUER_ID'] = _out_chauffuer
        self.fields['OUT_CHAUFFUER_PURP'] = _out_chauffer_purp
        self.fields['OUT_CHAUFFUER_PTYPE'] = _out_chauffer_ptype
        self.fields['INB_ESCORT_TYPE'] = _inb_escort_type
        self.fields['INB_CHAUFFUER_ID'] = _inb_chauffuer
        self.fields['INB_CHAUFFUER_PURP'] = _inb_chauffer_purp
        self.fields['INB_CHAUFFUER_PTYPE'] = _inb_chauffer_ptype


    def set_escorting_fields(self):

        #initialize set of escorted hh members to empty set
        _escort_pers_set = set()
        _out_escorting = 0
        _inb_escorting = 0
        _out_escort_type = np.NaN
        _inb_escort_type = np.NaN
        _tour_purp = self.get_purp()
        _perObj = self.per_obj
        _pertype = _perObj.fields['PERSONTYPE']
        for _trip in self.trips:
            #set union with the trip's list of escorted individuals
            _escort_pers_set = _escort_pers_set | set(_trip.escort_pers)

            #number of escorting episodes in outbound/inbound direction
            #check if ESCORTING key exists, otherwise no escortin on this trip
            if 'ESCORTING' in _trip.fields:
                if (_trip.fields['IS_INBOUND']==0) & (_trip.fields['ESCORTING']!=NewEscort['NEITHER']):
                    _out_escorting = _out_escorting + 1
                if (_trip.fields['IS_INBOUND']==1) & (_trip.fields['ESCORTING']!=NewEscort['NEITHER']):
                    _inb_escorting = _inb_escorting + 1

        #code escorting type for outbound and inbound leg
        if  (not math.isnan(_tour_purp)) & (_out_escorting>0):
            if ((_tour_purp>0) & (_tour_purp<=3)) | (_tour_purp==10):
                _out_escort_type = NewEscortType['RIDE_SHARING']
            elif (_tour_purp>3) & (_tour_purp<10):
                _out_escort_type = NewEscortType['PURE_ESCORT']
        elif ((_pertype==4) | (_pertype==5)) & (_out_escorting>0):
            _out_escort_type = NewEscortType['PURE_ESCORT']
        elif (((_pertype > 0) & (_pertype <=3)) | (_pertype==6)) & (_out_escorting>0):
            _out_escort_type = NewEscortType['RIDE_SHARING']
        else:
            _out_escort_type = NewEscortType['NO_ESCORT']

        if (not math.isnan(_tour_purp)) & (_inb_escorting>0):
            if ((_tour_purp>0) & (_tour_purp<=3)) | (_tour_purp==10):
                _inb_escort_type = NewEscortType['RIDE_SHARING']
            elif (_tour_purp>3) & (_tour_purp<10):
                _inb_escort_type = NewEscortType['PURE_ESCORT']
        elif ((_pertype==4) | (_pertype==5)) & (_inb_escorting>0):
            _inb_escort_type = NewEscortType['PURE_ESCORT']
        elif (((_pertype > 0) & (_pertype <=3)) | (_pertype==6)) & (_inb_escorting>0):
            _inb_escort_type = NewEscortType['RIDE_SHARING']
        else:
            _inb_escort_type = NewEscortType['NO_ESCORT']

        _num_escorted = len(_escort_pers_set)
        _escorted_list = sorted(list(_escort_pers_set))

        #set fields
        self.fields['ESCORTING_TOUR'] = int(_num_escorted>0)
        self.fields['NUM_PERSONS_ESCORTED'] = _num_escorted
        self.fields['OUT_ESCORTING_TYPE'] = _out_escort_type
        self.fields['INB_ESCORTING_TYPE'] = _inb_escort_type
        self.fields['OUT_ESCORTING_EPISODES'] = _out_escorting
        self.fields['INB_ESCORTING_EPISODES'] = _inb_escorting
        for _i in range(0, _num_escorted):
            #TODO: as many fields are set as needed, but only 5(?) persons will be written out
            self.fields['ESCORT_PERS_'+str(_i+1)] = _escorted_list[_i]


    def _set_AW_subtours(self, prim_i):
        """ scan through trips in the tour to identify and extract any at-work subtours """
        """ prim_i: trips list index of the primary destination                         """
        _to_work = []           #list indices of the trips arriving at work
        _from_work = []         #list indices of the trips departing from work
        _num_subtours = 0       #counter initialized to 0

        for _i, _trip in enumerate(self.trips):
            #_trip = self.trips[_i]
            if _trip.fields['ORIG_PURP']==NewPurp["WORK"]:
                #found a trip leaving work
                _from_work.append(_i)
            if _trip.fields['DEST_PURP']==NewPurp["WORK"]:
                #found a trip going to work
                _to_work.append(_i)

        _num_trips_to_work = len(_to_work)
        _num_trips_from_work= len(_from_work)
        if _num_trips_to_work==_num_trips_from_work:
            if _num_trips_from_work>1:
                #there is at least 1 at-work subtour
                #drop the first trip to work from subsequent processing
                del _to_work[0]
                #drop the last trip to work from subsequent processing
                del _from_work[-1]
                #remaining items in the _to_work and _from_work lists mark the start and end of a subtour

                _trips_to_remove = []   #list indices of trips that constitute at-work subtour(s) and need to be removed from the parent tour
                for _start_i, _end_i in zip(_from_work, _to_work):       #process each subtour
                    #_start_i: list index of the trip leaving from work
                    #_end_i:   list index of the trip returning to work
                    _num_subtours = _num_subtours+1     #update counter
                    #create the subtour
                    _subtour_trips = self.trips[_start_i:_end_i+1]      #take a slice of the list that contains trips for the subtour
                    _subtour = self.per_obj.add_subtour(_subtour_trips) #create & add new tour to the person's list
                    _subtour.set_parent(self)                           #set the parent tour attributes for the subtour
                    _trips_to_remove.extend(_subtour_trips)             #add trips to the to-be-removed list
                    #keep record of the subtour's id
                    self.fields['CHILD_TOUR_ID_'+str(_num_subtours)] = _subtour.get_id()

                #all subtours have been created, now clean up the parent tour's trip list
                for _trip in _trips_to_remove:
                    self.trips.remove(_trip)
                #renumber the trips
                self.renumber_trips()
        elif self.fields['PARTIAL_TOUR']==NewPartialTour['NOT_PARTIAL']:
            self.log_error("to- and from-work trips not matching up")

        #TODO: provide warning msg when there are not enough place holders for child tour id (currently table allows for 3)
        #set field value
        self.fields['NUM_SUBTOURS'] = _num_subtours

        return _num_subtours

    def _find_prim_stop(self):
        if len(self.trips)==1:      #single trip in the tour ->loop?
            _prim_trip = 0
        elif self.get_partial_status()==NewPartialTour['NOT_PARTIAL']:
            _prim_trip = self._find_prim_by_score()
        else:
            _prim_trip = None

        return _prim_trip

    def _find_prim_by_score(self):
        d = [0,60,120,180,240,300,360,420,480]
        score = {   NewPurp['WORK']:         [8,4,2,1.5,1.4,1.3,1.2,1.1,1],
                    NewPurp['UNIVERSITY']:   [8,4,2,1.5,1.4,1.3,1.2,1.1,1],
                    NewPurp['SCHOOL']:       [8,4,2,1.5,1.4,1.3,1.2,1.1,1],
                    NewPurp['ESCORTING']:    [8,6,4,3.5,3.4,3.3,3.2,3.1,3],
                    NewPurp['SHOPPING']:     [10,6,4,3.5,3.4,3.3,3.2,3.1,3],
                    NewPurp['MAINTENANCE']:  [10,6,4,3.5,3.4,3.3,3.2,3.1,3],
                    NewPurp['EAT OUT']:      [12,7,5,4.5,4.4,4.3,4.2,4.1,4],
                    NewPurp['SOCIAL/VISIT']: [14,8,6,5.5,5.4,5.3,5.2,5.1,5],
                    NewPurp['DISCRETIONARY']:[10,6,4,3.5,3.4,3.3,3.2,3.1,3],
                    NewPurp['WORK-RELATED']: [8,4,2,1.5,1.4,1.3,1.2,1.1,1],
                    NewPurp['LOOP']:         [20,10,7,6.5,6.4,6.3,6.2,6.1,6],
                    NewPurp['OTHER']:        [20,10,7,6.5,6.4,6.3,6.2,6.1,6]
                 }

        ### scan through all but the last trip in tour to identify primary activity/destination
        #_prim_i = -1            #list item no. corresponding to the primary destination
        _prim_i = None            #list item no. corresponding to the primary destination
        _min_score = 99999999     #lowest score found so far
        _num_trips = len(self.trips)
        for _i in range(0, _num_trips-1):
            _trip = self.trips[_i]
            _stop_purp = _trip.fields["DEST_PURP"]
            (_hours, _minutes) = calculate_duration(_trip.fields["DEST_ARR_HR"],_trip.fields["DEST_ARR_MIN"],
                                                              _trip.fields["DEST_DEP_HR"],_trip.fields["DEST_DEP_MIN"])
            _stop_dur = convert2minutes(_hours,_minutes)
            _trip_dur = convert2minutes(_trip.fields["TRIP_DUR_HR"],_trip.fields["TRIP_DUR_MIN"])
            _i_score = np.interp(_stop_dur+2*_trip_dur, d, score[_stop_purp])

            # DH [02/11/20] Added optional distance dependence
            if USE_DISTANCE_IN_PRIMARY_LOCATION_SCORE:
                # print("HOME_DIST: ", _trip.fields['HOME_DIST'])
                _i_score = _i_score + max(2 - (_trip.fields['HOME_DIST']/30), 1)

            if _i_score < _min_score:
                _min_score = _i_score
                _prim_i = _i
        #destination of the _i'th trip in the list is the primary destination
        return _prim_i

#
    """
    def _find_prim_stop(self):
        ### scan through all but the last trip in tour to identify primary activity/destination
        _prim_i = -1        #list item no. corresponding to the primary destination
        _max_dur = -1       #maximum activity duration (at a stop) found so far
        _num_trips = len(self.trips)
        for _i in range(0, _num_trips-1):
            _trip = self.trips[_i]
            _stop_purp = _trip.fields["DEST_PURP"]
            if _stop_purp in [ NewPurp["WORK"], NewPurp["UNIVERSITY"], NewPurp["SCHOOL"] ]:
                #it is one of the mandatory activities
                _prim_i = _i
                #no need to continue scanning if a primary destination is found
                break
            else:
                #it is a non-mandatory activity
                (hours, minutes) = calculate_duration(_trip.fields["DEST_ARR_HR"],_trip.fields["DEST_ARR_MIN"],
                                                              _trip.fields["DEST_DEP_HR"],_trip.fields["DEST_DEP_MIN"])
                _stop_dur = convert2minutes(hours,minutes)
                if _stop_dur > _max_dur:
                    _max_dur = _stop_dur
                    _prim_i = _i
        #destination of the _i'th trip in the list is the primary destination
        return _prim_i
    """

    def print_header(fp):
        #fp.write(','.join(['%s' %field for field in self.fields.keys()])+'\n')
        _header=[]
        for _col_num, _col_name in sorted(TourCol2Name.items()):    #TODO: save a sorted copy of the dict to avoid repeated sorting
            _header.append(_col_name)
        fp.write(','.join(['%s' %name for name in _header])+'\n')

    def print_vals(self, fp):
        #fp.write(','.join(['%s' %value for value in self.fields.values()])+'\n')
        if 'ERROR' in self.fields:
            self.fields['ERROR'] = add_quote_char(self.fields['ERROR'])
        _vals = []
        for _col_num, _col_name in sorted(TourCol2Name.items()):
            if _col_name in self.fields:
                _vals.append(self.fields[_col_name])
            else:
                _vals.append(np.NAN)
        fp.write(','.join(['%s' %value for value in _vals])+'\n')

class Joint_ultrip:
    """ Joint episode class """
    def __init__(self, trip, num_tot_travelers, num_hh_mem, dep_time, arr_time, hh_travelers):
        self.parent_trip = trip
        self.depart_time = dep_time
        self.arrival_time = arr_time
        self.number_tot = int(num_tot_travelers)
        self.number_hh = int(num_hh_mem)            #number of household members (as reported) who traveled together
        self.travel_party = [int(pid) for pid in hh_travelers if pid>0]      #list of valid IDs of household members in the travel party
        self.travel_party.sort()
        self.jt_id = np.NAN         #ID assigned to each group of trips made together by multiple household members
        self.jtour_id = np.NAN      #ID assigned to each group of trips made together by multiple household members
        self.composition = np.NAN
        self.driver = np.NAN
        self.driver_tour = np.NAN
        self.driver_trip = np.NAN
        self.error_flag = False     #initialize error flag to false
        self.error_msg = ""
        #print("adding joint trip person={}, depart={}, number_hh={}, party={}".format(self.parent_trip.get_per_id(), self.parent_trip.get_depart_time(), self.number_hh, self.travel_party))
        #size of travel party consistent with number_hh?
        #if len(self.travel_party)!=self.number_hh:
        #    _dep_time = self.get_depart_time()
        #    _msg = "Joint trip departing at {}:{} reported by person {}: {} hh members reported, but {} people listed".format(
        #                                    _dep_time//60, _dep_time%60, self.parent_trip.get_per_id(), self.number_hh, len(self.travel_party))
        #    self.parent_trip.hh_obj.log_error(_msg)

    def add_driver(self, per_id, tour_id, trip_id):
        self.driver = per_id
        self.driver_tour = tour_id
        self.driver_trip = trip_id

    def get_depart_time(self):
        return self.depart_time

    def get_arrival_time(self):
        return self.arrival_time

    def get_num_tot_travelers(self):
        return self.number_tot

    def get_num_hh_travelers(self):
        return self.number_hh

    def get_hh_id(self):
        return self.parent_trip.get_hh_id()

    def get_per_id(self):
        return self.parent_trip.get_per_id()

    def get_tour(self):
        return self.parent_trip.tour_obj

    def get_travel_party(self):
        return self.travel_party

    def get_dest_escort_pudo(self):
        return self.parent_trip.get_dest_escort_pudo()

    def get_orig_escort_pudo(self):
        _cur_trip_id = self.parent_trip.get_id()
        _pudo = NewEscort['NEITHER']
        if _cur_trip_id>1:
            _prev_trip = self.parent_trip.tour_obj.get_trip(_cur_trip_id-1)
            _pudo = _prev_trip.get_dest_escort_pudo()
        return _pudo

    def get_orig_travel_party(self):
        _cur_trip_id = self.parent_trip.get_id()
        _party = []
        if _cur_trip_id>1:
            _prev_trip = self.parent_trip.tour_obj.get_trip(_cur_trip_id-1)
            if _prev_trip.get_is_joint():
                _party = _prev_trip.get_joint_descriptions()[-1].get_travel_party()
            else:
                _party = [self.get_per_id()]
        return _party

    def get_joint_ultrip_id(self):
        return self.jt_id

    def set_joint_ultrip_id(self, jt_id):
        self.jt_id = jt_id
        #self.parent_trip.add_joint_ultrip_ids(jt_id)    #update in the trip record

    def set_chauffuer(self, chauffuer_id):
        self.driver = chauffuer_id
        self.parent_trip.set_chauffuer(chauffuer_id)                #update in the trip record

    def set_composition(self, composition):
        self.composition = composition

    def set_jtour_id(self, jtour_id):
        #set its data member
        self.jtour_id = jtour_id
        #also set the parent tour as a fully joint tour
        self.parent_trip.tour_obj.set_fully_joint(jtour_id)

    def recode_travel_party(self, set_participants):
        list_participants = list(set_participants)
        list_participants.sort()
        if list_participants!=self.travel_party:
            self.parent_trip.log_recode("reset TRAVEL PARTY from {} to {}".format(self.travel_party, list_participants) )
            self.travel_party = list_participants

    def recode_number_travelers(self, number_travelers):
        if number_travelers != self.number_hh:
            self.parent_trip.log_recode("reset NUMBER OF PARTICIPANTS on trip from {} to {}".format(self.number_hh, number_travelers) )
            self.number_hh = number_travelers

    def log_error(self, err_msg=None):
        self.error_flag = True
        if err_msg:     #if there is an error message
            #pt = self.parent_trip
            #pt.hh_obj.log_error("\t Person#{} Tour#{} Trip#{}: ".format(pt.per_obj.get_id(), pt.tour_obj.get_id(), pt.trip_id)+err_msg)
            self.error_msg = self.error_msg + "E: " + err_msg
            self.parent_trip.log_error(err_msg)

    def print_header(fp):
        _header=["HH_ID", "PER_ID", "TOUR_ID", "TRIP_ID", "LEG_DEST_PLACENO", "JTRIP_ID",
                 "NUMBER_HH", "CHAUFFUER_ID",
                 "ORIG_DEP_HR", "ORIG_DEP_MIN", "DEST_ARR_HR", "DEST_ARR_MIN",
                 "DEST_PURP", "PARTY", "ERROR"]
        fp.write(','.join(['%s' %field for field in _header])+'\n')
    """
    def print_vals(self, fp):
        pt = self.parent_trip
        _vals=[pt.get_hh_id(), pt.get_per_id(), pt.get_tour_id(), pt.trip_id, pt.fields["DEST_PLACENO"], self.jt_id,
               self.number_hh, self.driver,
               pt.fields["ORIG_DEP_HR"], pt.fields["ORIG_DEP_MIN"], pt.fields["DEST_ARR_HR"], pt.fields["DEST_ARR_MIN"],
               pt.fields["DEST_PURP"]]
        fp.write(','.join(['%s' %value for value in _vals]))
        fp.write(','+'_'.join(['%s' %int(pid) for pid in sorted(self.travel_party)]))   #print travel party
        fp.write(','+self.error_msg+'\n')                                               #print error message
    """
    def print_vals(self, fp):
        pt = self.parent_trip
        _vals=[pt.get_hh_id(), pt.get_per_id(), pt.get_tour_id(), pt.trip_id, pt.fields["DEST_PLACENO"], self.jt_id,
               self.number_hh, self.driver,
               math.floor(self.depart_time/60), self.depart_time%60, math.floor(self.arrival_time/60), self.arrival_time%60,
               pt.fields["DEST_PURP"]]
        fp.write(','.join(['%s' %value for value in _vals]))
        fp.write(','+'_'.join(['%s' %int(pid) for pid in sorted(self.travel_party)]))   #print travel party
        fp.write(','+add_quote_char(self.error_msg)+'\n')                                               #print error message

    def print_header_unique(fp):
        _header=["HH_ID", "JTRIP_ID", "NUMBER_HH"]
        #PERSON_1 to PERSON_9
        for _i in range(1, 10):
            _header.append("PERSON_"+str(_i))
        _header.append("COMPOSITION")
        fp.write(','.join(['%s' %field for field in _header])+'\n')

    def print_vals_unique(self, fp):
        #write out hh id, joint trip id, and size of travel group
        fp.write('{},{},{}'.format(self.parent_trip.get_hh_id(), self.jt_id, self.number_hh))
        #write out IDs of people in the travel party
        _party = sorted(self.travel_party)
        for _i in range(0, self.number_hh):
            fp.write(',{}'.format(_party[_i]))
        #fill nan up to person 9
        for _i in range(self.number_hh, 9):
            fp.write(',nan')
        #write out group composition
        fp.write(',{}\n'.format(self.composition))

class Trip:
    """ Trip class """
    def __init__(self, hh_obj, per_obj, tour_obj, trip_id):
        """ instantiation of a Trip object """
        self.hh_obj = hh_obj
        self.per_obj = per_obj
        self.tour_obj = tour_obj
        self.trip_id = trip_id
        self.joint_descriptors = []
        self.escort_pers = []           #store the id(s) of hh members being picked up at the trip's origin or dropped off at the trip's destination
        self.fields = {'HH_ID':self.get_hh_id(), 'PER_ID':self.get_per_id(), 'TOUR_ID':self.get_tour_id(), 'TRIP_ID':self.trip_id}
        self.error_flag = False
        self.arr_times = []
        self.dep_times = []

    def get_joint_descriptions(self):
        return self.joint_descriptors

    def get_hh_id(self):
        return self.hh_obj.get_id()

    def get_per_id(self):
        return self.per_obj.get_id()

    def get_tour_id(self):
        return self.tour_obj.get_id()

    def get_id(self):
        return self.trip_id

    def is_orig_home(self):
        return self.fields['ORIG_PURP'] == NewPurp['HOME']

    def is_dest_home(self):
        return self.fields['DEST_PURP'] == NewPurp['HOME']

    def get_is_escorting(self):
        if 'DEST_PURP' in self.fields:   #make sure the key exists
            return self.fields['DEST_PURP']==NewPurp['ESCORTING']
        else:
            #TODO: better error handling?
            return False

    def get_is_joint(self):
        is_joint = False
        if 'JOINT' in self.fields:   #make sure the key exists
            if self.fields['JOINT']!=NewJointCategory['NOT-JOINT']:
                is_joint = True
        return is_joint

    def get_joint_status(self):
        return self.fields['JOINT']

    def get_jtripID(self):
        if 'JTRIP_ID' in self.fields: #make sure the key exists
            return self.fields['JTRIP_ID']

    def get_dest_escort_pudo(self):
        if 'DEST_ESCORTING' in self.fields:
            return self.fields['DEST_ESCORTING']
        else:
            return NewEscort['NEITHER']

    def get_chauffuer(self):
        if 'CHAUFFUER_ID' in self.fields:
            return self.fields['CHAUFFUER_ID']
        else:
            return None

    def get_escorted(self):
        if 'ESCORTED' in self.fields:
            return self.fields['ESCORTED']
        else:
            return NewEscort['NEITHER']

    def get_depart_time(self):
        """ return the departure time at trip origin in minutes """
        _hr = self.fields["ORIG_DEP_HR"]
        _min = self.fields["ORIG_DEP_MIN"]
        return int(_hr*60+_min)

    def get_arrival_time(self):
        """ return the arrival time at trip destination in minutes """
        _hr = self.fields["DEST_ARR_HR"]
        _min = self.fields["DEST_ARR_MIN"]
        return int(_hr*60+_min)

    def get_orig_purpose(self):
        return self.fields['ORIG_PURP']

    def get_dest_purpose(self):
        return self.fields['DEST_PURP']

    def get_place_number(self):
        return self.fields['DEST_PLACENO']

    def get_dist(self):
        if 'DIST' in self.fields:
            if self.fields['DIST'] >= 0:
                return self.fields['DIST']
            else:
                return np.NAN
        else:
            return np.NAN

    """
    def get_hhmem(self):
        return self.fields['HHMEM']
    """
    def set_id(self, new_id):
        self.trip_id = new_id
        self.fields['TRIP_ID'] = int(new_id)    #make sure that output field is updated too

    def set_jt_grouping_status(self):
        #check if all joint legs of this trip are grouped (i.e. has a JTIP_ID assigned)
        #if so, then this is a JOINT-GROUPED trip; otherwise, it is left as a JOINT trip
        _all_grouped = True
        _jt_ids = []
        for jt in self.joint_descriptors:
            _jt_ids.append(jt.get_joint_ultrip_id())
            if not (jt.get_joint_ultrip_id()>0):
                #found one leg that is not grouped
                _all_grouped = False
        if len(self.joint_descriptors)==0:
            self.fields['JOINT'] = NewJointCategory['NOT-JOINT']
        elif _all_grouped:
            self.fields['JOINT'] = NewJointCategory['JOINT-GROUPED']
            self.fields['JTRIP_ID'] = add_quote_char(','.join(['%s' %id for id in _jt_ids]))
        else:
            self.fields['JOINT'] = NewJointCategory['JOINT']

    def set_per_type(self, per_type):
        self.fields['PERSONTYPE'] = per_type

    def set_chauffuer(self, driver_id):
        self.fields['CHAUFFUER_ID'] = driver_id

    def set_escorting(self, pudo, escort_set):

        _num_escorted = len(escort_set)
        self.escort_pers = sorted(list(escort_set))
        #set fields
        self.fields['ESCORTING'] = pudo
        self.fields['NUM_PERSONS_ESCORTED'] = _num_escorted
        for _i in range(0, _num_escorted):
            #TODO: as many fields as needed are set, but only 5(?) persons will be written out
            self.fields['ESCORT_PERS_'+str(_i+1)] = self.escort_pers[_i]

    def set_escorted(self, escorted_code):
        self.fields['ESCORTED'] = escorted_code

    def set_stop_number_on_half_tour(self, stop_num):
        self.fields['STOP_NUM_HTOUR'] = stop_num

    def set_trip_direction(self):
        outbound_stops = self.tour_obj.get_outbound_stops()
        is_inbound = 0                              #1 if trip is in inbound direction, else 0
        if self.trip_id > (outbound_stops+1):
            is_inbound=1
        self.fields['IS_INBOUND']=is_inbound

    def set_tour_attributes(self):
        is_subtour = self.tour_obj.get_is_subtour()
        outbound_stops = self.tour_obj.get_outbound_stops()
        inbound_stops = self.tour_obj.get_inbound_stops()
        num_trips = self.tour_obj.get_num_trips()

        self.fields['SUBTOUR'] = is_subtour         #1 if trip is on an At-Work Subtour, else 0

        #is_inbound = 0                              #1 if trip is in inbound direction, else 0
        #if self.trip_id > (outbound_stops+1):
        #    is_inbound=1
        #self.fields['IS_INBOUND']=is_inbound

        is_inbound = self.fields['IS_INBOUND']
        #Number of trips on journey to or from primary destination
        if is_inbound==1:
            self.fields['TRIPS_ON_JOURNEY'] = inbound_stops+1
        else:
            self.fields['TRIPS_ON_JOURNEY'] = outbound_stops+1

        #number of trips on the tour
        self.fields['TRIPS_ON_TOUR'] = num_trips

        if self.trip_id==1:
            self.fields['ORIG_IS_TOUR_ORIG'] = 1    #1 if trip origin is tour anchor/origin, else 0
        else:
            self.fields['ORIG_IS_TOUR_ORIG'] = 0

        if self.trip_id==(outbound_stops+2):
            self.fields['ORIG_IS_TOUR_DEST'] = 1    #1 if trip origin is tour primary destination, else 0
        else:
            self.fields['ORIG_IS_TOUR_DEST'] = 0

        if self.trip_id==(outbound_stops+1):
            self.fields['DEST_IS_TOUR_DEST'] = 1    #1 if trip destination is tour primary destination, else 0
        else:
            self.fields['DEST_IS_TOUR_DEST'] = 0

        if self.trip_id==(num_trips):
            self.fields['DEST_IS_TOUR_ORIG'] = 1    #1 if trip destination is tour anchor/origin
        else:
            self.fields['DEST_IS_TOUR_ORIG'] = 0

        self.fields['FULLY_JOINT'] = self.tour_obj.get_is_fully_joint()
        self.fields['PARTIAL_TOUR'] = self.tour_obj.get_partial_status()

        self.fields['TOURMODE'] = self.tour_obj.get_mode()
        self.fields['TOURPURP'] = self.tour_obj.get_purp()


    def _set_transit_attributes(self, df):
        """Process the given DataFrame object to determine the various transit-related trip attributes"""
        #initialize
        _transit_leg_count = 0
        _access_mode = []
        _egress_mode = []
        _last_row = len(df)-1

        #First, loop through all access (non-transit) legs
        _cur_row = 1    #not 0 because the leg starts at row[0] and ends at row[1]
        _mode = df['MODE'].iloc[_cur_row]
        while (~_mode.isin(SurveyTransitModes)):
            _access_mode.append(_mode)
            _cur_row = _cur_row+1
            _mode = df['MODE'].iloc[_cur_row]
            ### end while

        #if no access leg was found
        if len(_access_mode)==0:
            #assume access was by walk
            _access_mode.append(SurveyMode['WALK']) 

        #Second, loop through transit legs
        #Need to make sure row pointer does not go past the end of the rows (in the event of missing egress legs)
        while (_mode.isin(SurveyTransitModes)):
            _transit_leg_count = _transit_leg_count+1
            _prev_row = _cur_row-1
            if (_transit_leg_count==1):
                #if this is the first transit legs, the previous place is the boarding stop
                self.fields['BOARDING_PLACENO'] = df['PLANO'].iloc[_prev_row]
                self.fields['BOARDING_PNAME']   = add_quote_char(df['PNAME'].iloc[_prev_row])
                self.fields['BOARDING_X']       = df['XCORD'].iloc[_prev_row]
                self.fields['BOARDING_Y']       = df['YCORD'].iloc[_prev_row]
            else:
                #this is the 2+ transit leg, the previous place is a transfer stop
                #transfer stop count = transit leg count - 1
                self.fields['XFER_'+str(_transit_leg_count-1)+'_PLACENO'] = df['PLANO'].iloc[_prev_row]
                self.fields['XFER_'+str(_transit_leg_count-1)+'_PNAME']   = add_quote_char(df['PNAME'].iloc[_prev_row])
                self.fields['XFER_'+str(_transit_leg_count-1)+'_X']       = df['XCORD'].iloc[_prev_row]
                self.fields['XFER_'+str(_transit_leg_count-1)+'_Y']       = df['YCORD'].iloc[_prev_row]
            #current transit leg
            #BMP: transit route and mode info not needed
            self.fields['TRANSIT_ROUTE_'+str(_transit_leg_count)] = '' #TODO: type check. add_quote_char() if string
            self.fields['TRANSIT_MODE_'+str(_transit_leg_count)]  = ''
            #point to the next row
            _cur_row = _cur_row+1
            if (_cur_row>_last_row):
                break;
            else:   #look up next mode
                _mode = df['MODE'].iloc[_cur_row]
            ### end while

        #number of transfers is number of transit legs - 1
        _num_transfers = _transit_leg_count-1
        self.fields['TRANSIT_NUM_XFERS'] = _num_transfers
        if _num_transfers>MAX_XFERS:
            self.log_warning("found {} transfers, but only {} were written out".format(_num_transfers, MAX_XFERS))

        #PLACE of the previous row is the ALIGHTING location
        self.fields['ALIGHTING_PLACENO'] = df['PLANO'].iloc[_cur_row-1]
        self.fields['ALIGHTING_PNAME']   = add_quote_char(df['PNAME'].iloc[_cur_row-1])
        self.fields['ALIGHTING_X']       = df['XCORD'].iloc[_cur_row-1]
        self.fields['ALIGHTING_Y']       = df['YCORD'].iloc[_cur_row-1]

        #Lastly, loop through remaining, egress legs
        while (_cur_row<=_last_row):
            _egress_mode.append(df['MODE'].iloc[_cur_row])
            #point to the next place entry
            _cur_row = _cur_row+1
            ### end while

        #if no egress leg was found
        if len(_egress_mode)==0:
            #assume access was by walk
            _egress_mode.append(SurveyMode['WALK']) #note: OHAS code for walk mode is 1

        return (_access_mode, _egress_mode)
    ###### end of _set_transit_attributes() ######

    def _find_best_transit_mode(self, df):
        # Not used for CMAP?
        _best_transit = 'WALK-TRANSIT'   #initialize to 

        #start from row#1, select the rows corresponding to transit mode use
        df_transit = df.iloc[1:]
        df_transit = df_transit[df_transit['MODE'].isin(SurveyTransitModes)]
        #transform TBUS values to transit type labels; put into a Pandas Series
        _transit_types = df_transit['TBUS'].apply(lambda x: SurveyTbus2TransitType[x])

        #promote to LRT or BRT if such modes were used
        if 'LRT' in _transit_types:
            _best_transit = 'LRT'
        elif 'BRT' in _transit_types:
            _best_transit = 'BRT'

        return _best_transit

    def _set_parking_attributes(self, df):
        """ determine parking location and related attributes for auto trips ending with non-auto, access mode (most likely walk) """
        _modes_col = df['MODE'].iloc[1:]    #note: skip row#0
        #locate the auto segments of the trip
        _auto_idx = np.where((_modes_col==SurveyMode['SOV'])|(_modes_col==SurveyMode['HOV2'])|(_modes_col==SurveyMode['HOV3']))
        _auto_idx_last  = np.max(_auto_idx)         #the last row where auto is used
        if _auto_idx_last < (len(_modes_col)-1):    #is this the last row for the trip?
            #found non-auto modes used before arriving at trip destination
            #location where the last auto mode used is assumed to be the parking location
            self.fields['PARKING_PLACENO']  = df['PLANO'].iloc[1+_auto_idx_last]    #add 1 because row#0 was skipped earlier
            self.fields['PARKING_PNAME']    = add_quote_char(df['PNAME'].iloc[1+_auto_idx_last])
            self.fields['PARKING_X']        = df['XCORD'].iloc[1+_auto_idx_last]
            self.fields['PARKING_Y']        = df['YCORD'].iloc[1+_auto_idx_last]


    def _set_auto_occupancy(self, df):
        """ determine the number of people traveling together for an auto trip """
        _auto_occ = int(0)
        #note that mode check starts from row #1 of the DataFrame
        #thus, need to add 1 back to narray index to get back to the row number
        _modes_array = df['DRIVER'].iloc[1:]
        #locate the DRIVER segments of the trip, if any
        _auto_idx = np.where(_modes_array==SurveyDriver['DRIVER'])[0]
        if np.size(_auto_idx)==0:
            #person was not a driver
            #locate the PASSENGER segments of the trip, if any
            _auto_idx = np.where(_modes_array==SurveyDriver['PASSENGER'])[0]
            #if np.size(_auto_idx)==0:
            #    #person was not a passenger
            #    #locate the CAR/VANPOOL segments of the trip, if any
            #    _auto_idx = np.where(_modes_array==SurveyMode['CAR/VANPOOL'])[0]

        if np.size(_auto_idx)==0:
            #trip did not involve any of the 3 above auto modes
            self.log_error("no auto-related segment found when attempting to calculate auto occupancy")
        else:
            _auto_occ = df['TOTTR'].iloc[1+_auto_idx[0]]        #add 1 to convert back to row number

        return _auto_occ

    def _calc_purpose(self, old_purp_code, old_place_no, old_place_name, dur_hr, dur_min):
        """ derive the new purpose code from input data purpose code; resolve inconsistencies between activity purpose and person status """
        _new_purp = -1
        if old_purp_code in SurveySchoolPurp:
            #school related activity
            _pertype = self.per_obj.get_per_type()
            if _pertype==NewPerType['US']:
                #person is an university student
                _new_purp = NewPurp['UNIVERSITY']
            elif _pertype in [NewPerType['DS'],NewPerType['ND']]:
                #person is a driving or non-driving age student
                _new_purp = NewPurp['SCHOOL']
            elif _pertype==NewPerType['PS']:
                #person is a pre-schooler, who may or may not be a student
                _new_purp = NewPurp['SCHOOL']
                if self.per_obj.get_student_category()==NewStuCategory['NON-STUDENT']:
                    #inconsistent with non-student status
                    self.per_obj.recode_student_category(NewStuCategory['SCHOOL'],
                        "found school activity (PLANO={}, PNAME={}) for non-student preschooler; reset STU_CAT to SCHOOL".format(old_place_no, old_place_name))

            else: #person types: FW, PW, NW, RE
                _new_purp = NewPurp['UNIVERSITY']
                self.per_obj.recode_student_category(NewStuCategory['UNIVERSITY'],
                        "found school activity (PLANO={}, PNAME={}) for Pertype={}; reset STU_CAT to UNIVERSITY".format(old_place_no, old_place_name, _pertype))
                if _pertype in [ NewPerType['PW'], NewPerType['NW'], NewPerType['RE'] ] : #person types PW,NW,RE by definition are not supposed to have school activities
                    self.per_obj.recode_per_type(NewPerType['US'],
                                "found school activity (PLANO={}, PNAME={}) for Pertype={}; reset PERSONTYPE to US".format(old_place_no, old_place_name, _pertype))

        else:
            _new_purp = SurveyTpurp2Purp[old_purp_code]
            if _new_purp in [ NewPurp['WORK'], NewPurp['WORK-RELATED'] ]:
                #found work or work-related activity, check if worker
                _pertype = self.per_obj.get_per_type()
                if _pertype in [ NewPerType['NW'], NewPerType['RE'] ]:
                    if convert2minutes(dur_hr,dur_min)>MAX_VOLUNTEER_MINUTES:
                        #if duration of work activity is over xxx hours, recode person as a part time worker and emp category as part time
                        self.per_obj.recode_emp_category(NewEmpCategory['PARTTIME'],
                            "found work or work-related activity (PLANO={}, PNAME={}) for Pertype={}; reset EMP_CAT to PARTTIME".format(old_place_no, old_place_name, _pertype))
                        self.per_obj.recode_per_type(NewPerType['PW'],
                            "found work or work-related activity (PLANO={}, PNAME={}) for Pertype={}; reset PERSONTYPE to PW".format(old_place_no, old_place_name, _pertype))
                    else:
                        #consider this as volunteer work, recode to discretionary purpose
                        _new_purp = NewPurp['DISCRETIONARY']
                if _pertype in [ NewPerType['ND'],NewPerType['PS'] ] :
                    #found work activity for non-driving age child; tag as an error
                    #self.per_obj.log_error("found work or work-related activity (PLANO={}, PNAME={}) for non-driving age child".format(old_place_no, old_place_name, _pertype))
                    #consider this as volunteer work, recode to discretionary purpose
                    _new_purp = NewPurp['DISCRETIONARY']
        return _new_purp

    def populate_attributes(self, df):
        """ determine trip attributes based on PLACE records in a DataFrame object"""


        #trip origin: 1st place on trip
        self.fields['ORIG_PLACENO'] = df['PLANO'].iloc[0]
        self.fields['ORIG_X']       = df['XCORD'].iloc[0]
        self.fields['ORIG_Y']       = df['YCORD'].iloc[0]
        self.fields['ORIG_ARR_HR']  = df['ARR_HR'].iloc[0]
        self.fields['ORIG_ARR_MIN'] = df['ARR_MIN'].iloc[0]
        self.fields['ORIG_DEP_HR']  = df['DEP_HR'].iloc[0]
        self.fields['ORIG_DEP_MIN'] = df['DEP_MIN'].iloc[0]

        #trip destination: last place on trip
        _last_row = len(df)-1
        self.fields['DEST_PLACENO'] = df['PLANO'].iloc[_last_row]
        self.fields['DEST_X']       = df['XCORD'].iloc[_last_row]
        self.fields['DEST_Y']       = df['YCORD'].iloc[_last_row]
        self.fields['DEST_ARR_HR']  = df['ARR_HR'].iloc[_last_row]
        self.fields['DEST_ARR_MIN'] = df['ARR_MIN'].iloc[_last_row]
        self.fields['DEST_DEP_HR']  = df['DEP_HR'].iloc[_last_row]
        self.fields['DEST_DEP_MIN'] = df['DEP_MIN'].iloc[_last_row]

        # DH [02/21/2020] Added TAZ information available in pre-processing step
        self.fields['ORIG_TAZ'] = df['TAZ'].iloc[0]
        self.fields['DEST_TAZ'] = df['TAZ'].iloc[_last_row]

        #arrival and departure time along all segments of the trip
        #calculate trip duration based on departure at origin and arrival at destination
        (self.fields['TRIP_DUR_HR'], self.fields['TRIP_DUR_MIN']) = calculate_duration(self.fields['ORIG_DEP_HR'], self.fields['ORIG_DEP_MIN'],
                                                                                       self.fields['DEST_ARR_HR'], self.fields['DEST_ARR_MIN'])
        self.fields['ORIG_PURP']    = self._calc_purpose(df['TPURP'].iloc[0],
                                                         df['PLANO'].iloc[0],
                                                         df['PNAME'].iloc[0],
                                                         self.fields['TRIP_DUR_HR'],
                                                         self.fields['TRIP_DUR_MIN'])

        self.fields['DEST_PURP']    = self._calc_purpose(df['TPURP'].iloc[_last_row],
                                                         df['PLANO'].iloc[_last_row],
                                                         df['PNAME'].iloc[_last_row],
                                                         self.fields['TRIP_DUR_HR'],
                                                         self.fields['TRIP_DUR_MIN'])

        #calculate time window bin number
        self.fields['ORIG_ARR_BIN'] = convert2bin(self.fields['ORIG_ARR_HR'],self.fields['ORIG_ARR_MIN'])
        self.fields['ORIG_DEP_BIN'] = convert2bin(self.fields['ORIG_DEP_HR'],self.fields['ORIG_DEP_MIN'])
        self.fields['DEST_ARR_BIN'] = convert2bin(self.fields['DEST_ARR_HR'],self.fields['DEST_ARR_MIN'])
        self.fields['DEST_DEP_BIN'] = convert2bin(self.fields['DEST_DEP_HR'],self.fields['DEST_DEP_MIN'])
        self.fields['TRIP_DUR_BIN'] = 1 + self.fields['DEST_ARR_BIN'] - self.fields['ORIG_DEP_BIN']

        #check for loop trips, i.e. purposes at both ends are HOME
        #recode destination purpose as LOOP
        if (self.fields['ORIG_PURP']==NewPurp['HOME']) & (self.fields['DEST_PURP']==NewPurp['HOME']):
            self.fields['DEST_PURP']=NewPurp['LOOP']
            self.log_warning("RECODE: Destination Purpose of HOME-HOME trip recoded as LOOP")


        #determine trip mode & related attributes
        #note that mode check is done from row #1 (not row #0) to the last row
        #if a driver for any segment of the trip
        # _toll_paid     = np.any(df['TOLL_NO'].iloc[1:]==SurveyToll['TOLL'])
        _min_tottr     = np.min(df['TOTTR'].iloc[1:])
        _max_tottr     = np.max(df['TOTTR'].iloc[1:])
        _is_driver     = np.any(df['DRIVER'].iloc[1:]==SurveyDriver['DRIVER'])  # redundant with get_is_driver ?
        _is_passenger  = np.any(df['DRIVER'].iloc[1:]==SurveyDriver['PASSENGER']) 
        _is_sov        = np.any(df['MODE'].iloc[1:]==SurveyMode['SOV'])
        _is_hov2       = np.any(df['MODE'].iloc[1:]==SurveyMode['HOV2'])
        _is_hov3       = np.any(df['MODE'].iloc[1:]==SurveyMode['HOV3'])
        
        _is_pnr_access  =  np.any(df['MODE'].iloc[1:]==SurveyMode['PNR-TRANSIT'])
        _is_knr_access  =  np.any(df['MODE'].iloc[1:]==SurveyMode['KNR-TRANSIT'])
        _is_walk_access  =  np.any(df['MODE'].iloc[1:]==SurveyMode['WALK-TRANSIT'])
        _is_tnc_access  =  np.any(df['MODE'].iloc[1:]==SurveyMode['TNC-TRANSIT'])

        _is_transit =  (np.any(df['MODE'].iloc[1:]==SurveyMode['TRANSIT'])) | \
                         (np.any(df['MODE'].iloc[1:]==SurveyMode['PNR-TRANSIT'])) | \
                         (np.any(df['MODE'].iloc[1:]==SurveyMode['KNR-TRANSIT'])) | \
                         (np.any(df['MODE'].iloc[1:]==SurveyMode['TNC-TRANSIT'])) | \
                        (np.any(df['MODE'].iloc[1:]==SurveyMode['WALK-TRANSIT']))

        _is_auto       = 1 if ((_is_sov==1)|(_is_hov2==1)|(_is_hov3==1)) else 0

        _is_sch_bus    = np.any(df['MODE'].iloc[1:]==SurveyMode['SCHOOLBUS'])      #if any segment of the trip used school bus
        _is_bike       = np.any(df['MODE'].iloc[1:]==SurveyMode['BIKE'])          #if any segment of the trip used bike
        _is_walk       = np.any(df['MODE'].iloc[1:]==SurveyMode['WALK'])          #if any segment of the trip used walk
        _is_taxi       = np.any(df['MODE'].iloc[1:]==SurveyMode['TAXI'])          #if any segment of the trip used txi
        _is_tnc_reg    = np.any(df['MODE'].iloc[1:]==SurveyMode['TNC-REG']) # if any segment of the trip used tnc
        _is_tnc_pool   = np.any(df['MODE'].iloc[1:]==SurveyMode['TNC-POOL']) # if any segment of the trip used tnc
        _is_auto       = 1 if _is_taxi else _is_auto
        _auto_occ      = 0  # initialize to 0
        _output_mode   = 0
        _access_mode   = 0
        
        _output_mode = df['MODE'].iloc[_last_row]

        if _is_transit:
            if(np.any(df['MODE'].iloc[1:]==SurveyMode['PNR-TRANSIT'])):
                _access_mode = 'PNR'
            elif(np.any(df['MODE'].iloc[1:]==SurveyMode['KNR-TRANSIT'])):
                _access_mode = 'KNR'
            elif(np.any(df['MODE'].iloc[1:]==SurveyMode['WALK-TRANSIT'])):
                _access_mode = 'WALK'
            else:
                _access_mode = 'WALK'

        if _is_sch_bus:
           _output_mode = NewTripMode['SCHOOLBUS']

        # transit 
        if _is_transit:
           #this is a transit trip
           
           if ((_is_auto)&(_is_driver)  | (np.any(df['ACCESS_MODE'].iloc[1:]=='PNR'))):
               _output_mode = NewTripMode['PNR-TRANSIT']
           elif (((_is_auto)&(not _is_driver)) | (np.any(df['ACCESS_MODE'].iloc[1:]=='KNR'))):
               _output_mode = NewTripMode['KNR-TRANSIT']
           elif(_is_tnc_pool | _is_tnc_reg | (np.any(df['ACCESS_MODE'].iloc[1:]=='TNR'))): 
               _output_mode = NewTripMode['TNC-TRANSIT']
           else:
               _output_mode = NewTripMode['WALK-TRANSIT']


        elif (_is_tnc_pool):
            _output_mode = NewTripMode['TNC-POOL']
        elif (_is_tnc_reg):
            _output_mode = NewTripMode['TNC-REG']
        elif _is_taxi:
           _output_mode = NewTripMode['TAXI']
        elif _is_bike:
           _output_mode = NewTripMode['BIKE']
        
       
        elif (_is_auto):
           #This is an auto trip
           if _is_hov3:
               _output_mode = NewTripMode['HOV3']
           elif _is_hov2:
               _output_mode = NewTripMode['HOV2']           
           elif _is_sov:
               _output_mode = NewTripMode['SOV']

        elif _is_walk:
           _output_mode = NewTripMode['WALK']
        

        elif _output_mode == 0: 
            _output_mode = NewTripMode['OTHER']


        #set the following fields for every trip, regardless of mode used
        self.fields['TRIPMODE'] = _output_mode
        #auto occupancy = travel party size if auto; 0 otherwise
        self.fields['AUTO_OCC'] = _max_tottr
        #driver status
        self.fields['ISDRIVER'] = int(_is_driver)

        #process joint travel
        _num_joint_episodes = 0
        for row_index in range(1,_last_row+1):
            #indicator for joint trip (with other household members)
            _hh_mem_on_trip = df['HHMEM'].iloc[row_index]   # HHMEM includes the current person

            if (_hh_mem_on_trip>1):
                #this is a joint episode:
                _num_joint_episodes = _num_joint_episodes+1
                #create and populate a joint travel descriptor object
                _new_joint_leg = Joint_ultrip(self,
                                              df['TOTTR'].iloc[row_index],
                                              _hh_mem_on_trip,
                                              convert2minutes(df['DEP_HR'].iloc[row_index-1], df['DEP_MIN'].iloc[row_index-1]),
                                              convert2minutes(df['ARR_HR'].iloc[row_index], df['ARR_MIN'].iloc[row_index]),
                                              [self.get_per_id(),df['PER1'].iloc[row_index],df['PER2'].iloc[row_index],df['PER3'].iloc[row_index],
                                              df['PER4'].iloc[row_index], df['PER5'].iloc[row_index], df['PER6'].iloc[row_index]])
                self.joint_descriptors.append(_new_joint_leg)
                if (df['DRIVER'].iloc[row_index]==SurveyDriver['DRIVER']):         #if a driver on this leg of the trip
                    _new_joint_leg.add_driver(self.get_per_id(), self.get_tour_id(), self.trip_id)

        if (_num_joint_episodes>0):
            #this is a joint trip:
            self.fields['JOINT'] = NewJointCategory['JOINT']
        else:
            self.fields['JOINT'] = NewJointCategory['NOT-JOINT']
        self.fields['NUM_UL_JTRIPS'] = _num_joint_episodes

        #if escorting, determine if it was picking-up or dropping-off
        #TODO: in MTC, DO & PU are coded the same
        # added a TOTTR check when DO & PU are coded the same [edited by BMP]
        _PU = 0
        _DO = 0
        for row_index in range(1,_last_row+1):
            if (df['TPURP'].iloc[row_index]==SURVEY_DO_PU_PURP_CODE) & (df['TOTTR'].iloc[row_index]<df['TOTTR_NEXT'].iloc[row_index]):
                _PU = _PU+1
            elif (df['TPURP'].iloc[row_index]==SURVEY_DO_PU_PURP_CODE) & (df['TOTTR'].iloc[row_index]>df['TOTTR_NEXT'].iloc[row_index]) & (df['TOTTR_NEXT'].iloc[row_index]>0):
                _DO = _DO+1
        if (_PU>0) & (_DO>0) :
            self.fields['DEST_ESCORTING'] = NewEscort['BOTH_PUDO']  #TODO: this would be an error?
        elif _PU>0:
            self.fields['DEST_ESCORTING'] = NewEscort['PICK_UP']
        elif _DO>0:
                self.fields['DEST_ESCORTING'] = NewEscort['DROP_OFF']
        else:
            self.fields['DEST_ESCORTING'] = NewEscort['NEITHER']

        if COMPUTE_TRIP_DIST:
            if np.all(df['Distance'].iloc[1:]>=0):          #if all distance measures are valid
                self.fields['DIST'] = np.sum(df['Distance'].iloc[1:])
            else:
                self.log_error("cannot compute trip distance")

        # DH [02/11/20] Adding HOME_DIST field
        if USE_DISTANCE_IN_PRIMARY_LOCATION_SCORE:
            if np.all(df['HOME_DIST'].iloc[1:]>=0):          #if all distance measures are valid
                self.fields['HOME_DIST'] = np.sum(df['HOME_DIST'].iloc[1:])
            else:
                self.log_error("cannot compute trip distance")

        return self.fields['JOINT']
    ###### end of populate_attributes() ######

    def log_error(self, err_msg=None):
        self.error_flag = True
        self.fields['ERROR'] = "E: "
        if err_msg:     #if there is an error message
            self.hh_obj.log_error("\t Person#{} Tour#{} Trip#{}: ".format(self.per_obj.get_id(), self.tour_obj.get_id(), self.trip_id)+err_msg)
            self.fields['ERROR'] = self.fields['ERROR']+err_msg

    def log_warning(self, msg):
        self.hh_obj.log_warning("\t <Warning> Person#{} Tour#{} Trip#{}: ".format(self.per_obj.get_id(), self.tour_obj.get_id(), self.trip_id)+msg)

    def log_recode(self, msg):
        self.per_obj.log_recode("TRIP_ID={} \t".format(self.trip_id)+msg)

    def print_header(fp):
        _header=[]
        for _col_num, _col_name in sorted(TripCol2Name.items()):    #TODO: save a sorted copy of the dict to avoid repeated sorting
            _header.append(_col_name)
        fp.write(','.join(['%s' %name for name in _header])+'\n')

    def print_vals(self, fp):
        if 'ERROR' in self.fields:
            self.fields['ERROR'] = add_quote_char(self.fields['ERROR'])

        _vals = []
        for _col_num, _col_name in sorted(TripCol2Name.items()):
            if _col_name in self.fields:
                _vals.append(self.fields[_col_name])
            else:
                _vals.append(np.NAN)
        fp.write(','.join(['%s' %value for value in _vals])+'\n')
