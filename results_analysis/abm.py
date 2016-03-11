#!/usr/bin/env python
'''
    abm.py
    Author: npeterson
    Revised: 3/11/16
    ---------------------------------------------------------------------------
    A class for reading ABM output files and matrix data into an SQL database
    for querying and summarization.

'''
import os
import sys
import copy
import csv
import math
import shelve
import sqlite3
import pandas as pd
from collections import Counter
from inro.emme.database import emmebank as _eb


class ABM(object):
    ''' A class for loading ABM model run output data into a SQLite database.
        Initialized with path (parent directory of 'model') and model run
        sample rate (default 1.00). '''

    # --- Properties ---
    facility_types = {
        'Arterial':   [1],
        'Expressway': [2, 4],
        'Ramp/Toll':  [3, 5, 7, 8],
        'Centroid':   [6]
    }

    modes = {
        1: 'Drive alone free',
        2: 'Drive alone pay',
        3: 'Shared ride 2 free',
        4: 'Shared ride 2 pay',
        5: 'Shared ride 3+ free',
        6: 'Shared ride 3+ pay',
        7: 'Walk',
        8: 'Bike',
        9: 'Walk to local transit',
        10: 'Walk to premium transit',
        11: 'Drive to local transit',
        12: 'Drive to premium transit',
        13: 'Taxi',
        14: 'School bus'
    }

    tod_by_index = [
        # List index corresponds to ABM time interval; value = TOD period
        None,                  # No CT-RAMP period 0
        1,1,1,                 # CT-RAMP periods 1-3: TOD 1 [3am, 6am)
        2,2,                   # CT-RAMP periods 4-5: TOD 2 [6am, 7am)
        3,3,3,3,               # CT-RAMP periods 6-9: TOD 3 [7am, 9am)
        4,4,                   # CT-RAMP periods 10-11: TOD 4 [9am, 10am)
        5,5,5,5,5,5,5,5,       # CT-RAMP periods 12-19: TOD 5 [10am, 2pm)
        6,6,6,6,               # CT-RAMP periods 20-23: TOD 6 [2pm, 4pm)
        7,7,7,7,               # CT-RAMP periods 24-27: TOD 7 [4pm, 6pm)
        8,8,8,8,               # CT-RAMP periods 28-31: TOD 8 [6pm, 8pm)
        1,1,1,1,1,1,1,1,1,1,1  # CT-RAMP periods 32-42: TOD 1 [8pm, 3am)
    ]

    tod_minutes = {
        # Length of each TOD period in minutes
        1: 600.0,
        2: 60.0,
        3: 120.0,
        4: 60.0,
        5: 240.0,
        6: 120.0,
        7: 120.0,
        8: 120.0
    }

    transit_modes = {
        'M': 'Metra Rail',
        'C': 'CTA Rail',
        'B': 'CTA Bus (Regular)',
        'E': 'CTA Bus (Express)',
        'L': 'Pace Bus (Local)',
        'P': 'Pace Bus (Regular)',
        'Q': 'Pace Bus (Express)'
    }

    zones = xrange(1, 1945)

    zone_groups = {
        'Chicago':      xrange(   1,  310),  # xrange upper-bounds are *exclusive*
        'Cook Balance': xrange( 310,  855),
        'McHenry':      xrange( 855,  959),
        'Lake':         xrange( 959, 1134),
        'Kane':         xrange(1134, 1279),
        'DuPage':       xrange(1279, 1503),
        'Will':         xrange(1503, 1691),
        'Kendall':      xrange(1691, 1712),
        'IL Balance':   xrange(1712, 1836),  # 1712 & 1752 are in CMAP MPO boundary
        'Indiana':      xrange(1836, 1910),
        'Wisconsin':    xrange(1910, 1945)
    }


    # --- Init ---
    def __init__(self, abm_dir, sample_rate=1.00, build_db=False):
        self.dir = abm_dir
        self.sample_rate = sample_rate
        self.name = os.path.basename(self.dir)
        self._input_dir = os.path.join(self.dir, 'model', 'inputs')
        self._output_dir = os.path.join(self.dir, 'model', 'outputs')
        self._emmebank_path = os.path.join(self.dir, 'model', 'CMAP-ABM', 'Database', 'emmebank')
        self._TEST_DIR = r'D:\workspace\Temp\ABM'                               ########## REMOVE LATER ##########
        self._db = os.path.join(self._TEST_DIR, '{0}.db'.format(self.name))     ########## CHANGE LATER ##########
        if build_db and os.path.exists(self._db):
            print 'Removing existing database...'
            os.remove(self._db)
        if not build_db and not os.path.exists(self._db):
            raise ValueError('SQLite database {0} does not yet exist. Please set build_db=True.'.format(self._db))

        # Set CT-RAMP CSV paths
        self._tap_attr_csv = os.path.join(self._input_dir, 'tap_attributes.csv')
        self._tap_lines_csv = os.path.join(self._output_dir, 'tapLines.csv')
        self._hh_data_csv = os.path.join(self._output_dir, 'hhData_1.csv')
        self._pers_data_csv = os.path.join(self._output_dir, 'personData_1.csv')
        self._tours_indiv_csv = os.path.join(self._output_dir, 'indivTourData_1.csv')
        self._tours_joint_csv = os.path.join(self._output_dir, 'jointTourData_1.csv')
        self._trips_indiv_csv = os.path.join(self._output_dir, 'indivTripData_1.csv')
        self._trips_joint_csv = os.path.join(self._output_dir, 'jointTripData_1.csv')

        # Load TAP data
        print 'Loading TAP data into memory...'
        self.tap_zones = {}
        with open(self._tap_attr_csv, 'rb') as csvfile:
            r = csv.DictReader(csvfile)
            for d in r:
                tap = int(d['tap_id'])
                zone = int(d['taz09'])
                self.tap_zones[tap] = zone

        self.tap_modes = {}
        with open(self._tap_lines_csv, 'rb') as csvfile:
            r = csv.DictReader(csvfile)
            for d in r:
                tap = int(d['TAP'])
                tlines = d['LINES'].strip().split()
                tline_modes = [tline[0].upper() for tline in tlines]
                mode_counts = Counter(tline_modes)
                modeshare = {mode: count / float(len(tlines)) for mode, count in mode_counts.iteritems()}
                self.tap_modes[tap] = modeshare
                ### NOTE: not true modeshare, doesn't account for headway, tod, or actual rider choices

        # Create DB to store CT-RAMP output
        print 'Opening database ({0})...'.format(self._db)
        self.open_db()

        # Load data from CSVs
        # -- Households table
        print 'Processing households...'
        if build_db:
            self._con.execute('''CREATE TABLE Households (
                hh_id INTEGER PRIMARY KEY,
                sz INTEGER,
                size INTEGER
                )''')
            self._con.commit()
            self._insert_households(self._hh_data_csv)

        self.households = self._unsample(self._count_rows('Households'))
        print '{0:<20}{1:>10,.0f}'.format('-- Households:', self.households)

        # -- People table
        print 'Processing people...'
        if build_db:
            self._con.execute('''CREATE TABLE People (
                pers_id TEXT PRIMARY KEY,
                hh_id INTEGER,
                pers_num INTEGER,
                age INTEGER,
                gender TEXT,
                class_w_wtt INTEGER,
                class_w_pnr INTEGER,
                class_w_knr INTEGER,
                class_o_wtt INTEGER,
                class_o_pnr INTEGER,
                class_o_knr INTEGER,
                FOREIGN KEY (hh_id) REFERENCES Households(hh_id)
                )''')
            self._con.commit()
            self._insert_people(self._pers_data_csv)

        self.people = self._unsample(self._count_rows('People'))
        print '{0:<20}{1:>10,.0f}'.format('-- People:', self.people)

        # -- Tours table
        print 'Processing tours...'
        if build_db:
            self._con.execute('''CREATE TABLE Tours (
                tour_id TEXT PRIMARY KEY,
                hh_id INTEGER,
                participants TEXT,
                pers_num TEXT,
                is_joint BOOLEAN,
                category TEXT,
                purpose TEXT,
                sz_o INTEGER,
                sz_d INTEGER,
                tod_d INTEGER,
                tod_a INTEGER,
                mode INTEGER,
                FOREIGN KEY (hh_id) REFERENCES Households(hh_id)
                )''')
            self._con.execute('''CREATE TABLE PersonTours (
                ptour_id TEXT PRIMARY KEY,
                tour_id TEXT,
                hh_id INTEGER,
                pers_id TEXT,
                mode INTEGER,
                FOREIGN KEY (pers_id) REFERENCES People(pers_id),
                FOREIGN KEY (tour_id) REFERENCES Tours(tour_id),
                FOREIGN KEY (hh_id) REFERENCES Households(hh_id)
                )''')
            self._con.commit()
            self._insert_tours(self._tours_indiv_csv, is_joint=False)
            self._insert_tours(self._tours_joint_csv, is_joint=True)

            self._con.execute('CREATE INDEX IX_Tours_isjnt ON Tours (is_joint)')
            self._con.execute('CREATE INDEX IX_Tours_mode ON Tours (mode)')
            self._con.execute('CREATE INDEX IX_PTours_mode ON PersonTours (mode)')
            self._con.commit()

        self.tours_indiv = self._unsample(self._count_rows('Tours', 'is_joint=0'))
        self.tours_joint = self._unsample(self._count_rows('Tours', 'is_joint=1'))
        self.tours = self.tours_indiv + self.tours_joint
        self.person_tours = self._unsample(self._count_rows('PersonTours'))
        print '{0:<20}{1:>10,.0f}'.format('-- Tours (indiv):', self.tours_indiv)
        print '{0:<20}{1:>10,.0f}'.format('-- Tours (joint):', self.tours_joint)
        print '{0:<20}{1:>10,.0f}'.format('-- Tours (total):', self.tours)
        print '{0:<20}{1:>10,.0f}'.format('-- Person-Tours:', self.person_tours)

        # -- Trips table
        print 'Processing trips...'
        if build_db:
            self._con.execute('''CREATE TABLE Trips (
                trip_id TEXT PRIMARY KEY,
                tour_id TEXT,
                hh_id INTEGER,
                pers_num TEXT,
                is_joint BOOLEAN,
                inbound BOOLEAN,
                purpose_o TEXT,
                purpose_d TEXT,
                sz_o INTEGER,
                sz_d INTEGER,
                zn_o INTEGER,
                zn_d INTEGER,
                tap_o INTEGER,
                tap_d INTEGER,
                tod INTEGER,
                mode INTEGER,
                drive_time REAL,
                drive_distance REAL,
                drive_speed REAL,
                FOREIGN KEY (tour_id) REFERENCES Tours(tour_id),
                FOREIGN KEY (hh_id) REFERENCES Households(hh_id)
                )''')
            self._con.execute('''CREATE TABLE PersonTrips (
                ptrip_id TEXT PRIMARY KEY,
                ptour_id TEXT,
                trip_id TEXT,
                tour_id TEXT,
                hh_id INTEGER,
                pers_id TEXT,
                mode INTEGER,
                uclass INTEGER,
                FOREIGN KEY (pers_id) REFERENCES People(pers_id),
                FOREIGN KEY (trip_id) REFERENCES Trips(trip_id),
                FOREIGN KEY (tour_id) REFERENCES Tours(tour_id),
                FOREIGN KEY (ptour_id) REFERENCES PersonTours(ptour_id),
                FOREIGN KEY (hh_id) REFERENCES Households(hh_id)
                )''')
            self._con.commit()
            self._insert_trips(self._trips_indiv_csv, is_joint=False)
            self._insert_trips(self._trips_joint_csv, is_joint=True)

            self._con.execute('CREATE INDEX IX_Trips_isjnt ON Trips (is_joint)')
            self._con.execute('CREATE INDEX IX_Trips_mode ON Trips (mode)')
            self._con.execute('CREATE INDEX IX_PTrips_mode ON PersonTrips (mode)')
            self._con.commit()

        self.trips_indiv = self._unsample(self._count_rows('Trips', 'is_joint=0'))
        self.trips_joint = self._unsample(self._count_rows('Trips', 'is_joint=1'))
        self.trips = self.trips_indiv + self.trips_joint
        self.person_trips = self._unsample(self._count_rows('PersonTrips'))
        print '{0:<20}{1:>10,.0f}'.format('-- Trips (indiv):', self.trips_indiv)
        print '{0:<20}{1:>10,.0f}'.format('-- Trips (joint):', self.trips_joint)
        print '{0:<20}{1:>10,.0f}'.format('-- Trips (total):', self.trips)
        print '{0:<20}{1:>10,.0f}'.format('-- Person-Trips:', self.person_trips)

        # -- TransitSegs table
        print 'Processing transit segments...'
        if build_db:
            self._con.execute('''CREATE TABLE TransitSegs (
                tseg_id TEXT,
                tline_id TEXT,
                tline_desc TEXT,
                headway REAL,
                tseg_num INTEGER,
                inode INTEGER,
                jnode INTEGER,
                tod INTEGER,
                transit_mode TEXT,
                is_rail BOOLEAN,
                boardings REAL,
                allow_boardings BOOLEAN,
                passengers REAL,
                pass_hrs REAL,
                pass_mi REAL,
                PRIMARY KEY (tseg_id, tod)
                )''')
            self._con.commit()
            self._insert_tsegs()

            self._con.execute('CREATE INDEX IX_TSegs_alwbrd ON TransitSegs (allow_boardings)')
            self._con.commit()

        self.transit_segments = self._count_rows('TransitSegs')
        print '{0:<20}{1:>10,.0f}'.format('-- Transit Segments:', self.transit_segments)

        self.mode_share = self._get_mode_share()
        self.ptrips_by_class = self._get_ptrips_by_class()
        self.vmt_by_speed = self._get_vmt_by_speed()

        self.close_db()

        return None  ### End of ABM.__init__() ###


    def __str__(self):
        return '[ABM: {0} ({1:.0%} sample)]'.format(self.name, self.sample_rate)


    # --- Class methods ---
    @classmethod
    def _convert_time_period(cls, in_period, ctramp_to_emme=True):
        ''' Convert CT-RAMP time period to Emme time-of-day, or vice versa.
            Uses a list with values corresponding to Emme TOD and index values
            corresponding to CT-RAMP periods: all 30-minute intervals, except
            some in TOD 1 (overnight). '''
        if ctramp_to_emme:
            return cls.tod_by_index[in_period]
        else:
            return [index for index, tod in enumerate(cls.tod_by_index) if tod == in_period]


    @classmethod
    def _get_matrix_nums(cls, mode):
        ''' Return the matrix numbers for congested time (t) and distance (d)
            corresponding to driving mode (1-6). See ABM User Guide p.36. '''
        if mode == 1:  # SOV, no toll
            t, d = 175, 177
        elif mode == 2:  # SOV, toll
            t, d = 180, 183
        elif mode == 3:  # HOV2, no toll
            t, d = 185, 187
        elif mode == 4:  # HOV2, toll
            t, d = 190, 193
        elif mode == 5:  # HOV3+, no toll
            t, d = 195, 197
        elif mode == 6:  # HOV3+, toll
            t, d = 200, 203
        else:
            t, d = None, None
        return (t, d)


    @classmethod
    def _get_mode_str(cls, mode_num):
        ''' Return description of a mode code. '''
        return cls.modes[mode_num]


    # --- Instance methods ---
    def _clean_str(self, string):
        ''' Clean a string for database entry. '''
        return string.lower().replace('-', '').replace(' ', '')


    def close_db(self):
        ''' Close the database connection. '''
        return self._con.close()


    def _count_rows(self, table, where_clause=None):
        ''' Execute a SELECT COUNT(*) query on a table with optional where
            clause and return the integer result. '''
        query = 'SELECT COUNT(*) FROM {0}'.format(table)
        if where_clause:
            query += ' WHERE {0}'.format(where_clause)
        return float(self._con.execute(query).fetchone()[0])


    def export_line_boardings_csv(self, csv_path):
        ''' Export a CSV file containing rail boardings by line, and total bus
            boardings. '''
        rail_codes = {
            'cbl': 'CTA Blue',
            'cbr': 'CTA Brown',
            'cga': 'CTA Green',  # Ashland/63rd
            'cgc': 'CTA Green',  # Cottage Grove
            'cor': 'CTA Orange',
            'cpk': 'CTA Pink',
            'cpr': 'CTA Purple',
            'crd': 'CTA Red',
            'cye': 'CTA Yellow',
            'mbn': 'Metra BNSF',
            'mhc': 'Metra HC',
            'mme': 'Metra ME',
            'mmn': 'Metra MD-N',
            'mmw': 'Metra MD-W',
            'mnc': 'Metra NCS',
            'mnw': 'Metra UP-NW',
            'mri': 'Metra RI',
            'mss': 'NICTD SS',
            'msw': 'Metra SWS',
            'mun': 'Metra UP-N',
            'muw': 'Metra UP-W'
        }

        # Get boardings by line ID, split into rail and bus
        boardings_dict = self._get_boardings('LINE', split_rail=True, scale_runs=False)

        # Count total bus boardings
        bus_total = sum(b for b in boardings_dict['BUS'].itervalues())

        # Count rail boradings by line
        rail_by_line = {}
        for run, boardings in boardings_dict['RAIL'].iteritems():
            line_code = run[:3]
            line = rail_codes.get(line_code, line_code)  # Use line_code if not in rail_codes dict
            rail_by_line[line] = rail_by_line.get(line, 0) + boardings

        # Write results to CSV
        with open(csv_path, 'wb') as w:
            row = '{0},{1}\n'
            w.write(row.format('LINE', 'BOARDINGS'))
            w.write(row.format('All Buses', bus_total))
            for line, boardings in sorted(rail_by_line.iteritems()):
                w.write(row.format(line, boardings))

        return csv_path


    def export_od_matrix_csv(self, csv_path):
        ''' Export a CSV containing two matrices of person-trip O-D zone
            groups: one for autos and one for transit. '''
        mode_groups = {
            1: 'AUTO',      # Drive alone free
            2: 'AUTO',      # Drive alone pay
            3: 'AUTO',      # Shared ride 2 free
            4: 'AUTO',      # Shared ride 2 pay
            5: 'AUTO',      # Shared ride 3+ free
            6: 'AUTO',      # Shared ride 3+ pay
            7: None,        # Walk
            8: None,        # Bike
            9: 'TRANSIT',   # Walk to local transit
            10: 'TRANSIT',  # Walk to premium transit
            11: 'TRANSIT',  # Drive to local transit
            12: 'TRANSIT',  # Drive to premium transit
            13: 'AUTO',     # Taxi
            14: 'AUTO',     # School bus
        }

        # Initialize results matrices
        zngrp_order = ['Chicago', 'Cook Balance', 'DuPage', 'Kane', 'Kendall', 'Lake', 'McHenry', 'Will', 'IL Balance', 'Indiana', 'Wisconsin']
        dim = len(zngrp_order)
        od_mat = {
            'AUTO': [[0 for i in xrange(dim)] for j in xrange(dim)],
            'TRANSIT': [[0 for i in xrange(dim)] for j in xrange(dim)]
        }

        # Append person-trip counts to appropriate matrix cell
        self.open_db()
        trip_sql = (
            ''' SELECT Trips.zn_o, Trips.zn_d, Trips.mode, Tours.participants '''
            ''' FROM Trips LEFT JOIN Tours ON Trips.tour_id=Tours.tour_id '''
        )
        for zn_o, zn_d, mode, participants in self.query(trip_sql):
            auto_or_transit = mode_groups[mode]
            if not auto_or_transit:
                continue  # Ignore non-motorized trips
            n = self._unsample(len(participants.split()))
            for zngrp, zones in self.zone_groups.iteritems():
                if zn_o in zones:
                    zngrp_o = zngrp
                    break
            for zngrp, zones in self.zone_groups.iteritems():
                if zn_d in zones:
                    zngrp_d = zngrp
                    break
            i = zngrp_order.index(zngrp_o)
            j = zngrp_order.index(zngrp_d)
            od_mat[auto_or_transit][i][j] += n
        self.close_db()

        # Write results to CSV
        with open(csv_path, 'wb') as w:
            for mode, matrix in sorted(od_mat.iteritems()):
                w.write(mode + ',' + ','.join(zngrp + ' (D)' for zngrp in zngrp_order) + '\n')
                for i, zngrp in enumerate(zngrp_order):
                    w.write(zngrp + ' (O),' + ','.join(str(n) for n in matrix[i]) + '\n')
                w.write('\n')

        return csv_path


    def export_trip_purpose_csv(self, csv_path):
        ''' Export a CSV file containing the number of tours made for each
            purpose and, within them, the purpose of the individual trips. '''
        self.open_db()

        # Count person-tours by tour purpose
        tour_sql = ''' SELECT purpose, participants FROM Tours '''
        tour_counts = {}
        for tour_purpose, participants in self.query(tour_sql):
            n = self._unsample(len(participants.split()))
            tour_counts[tour_purpose] = tour_counts.get(tour_purpose, 0) + n

        # Count person-trips by tour purpose and trip destination purpose
        trip_counts = {tour_purpose: {} for tour_purpose in tour_counts.iterkeys()}
        trip_purposes = set()
        trip_sql = (
            ''' SELECT Tours.purpose, Tours.participants, Trips.purpose_d '''
            ''' FROM Trips LEFT JOIN Tours ON Trips.tour_id=Tours.tour_id '''
        )
        for tour_purpose, participants, trip_purpose in self.query(trip_sql):
            trip_purposes.add(trip_purpose)
            n = self._unsample(len(participants.split()))
            trip_counts[tour_purpose][trip_purpose] = trip_counts[tour_purpose].get(trip_purpose, 0) + n

        self.close_db()

        # Write results to CSV
        with open(csv_path, 'wb') as w:
            w.write('PERSON-TOUR PURPOSE,PERSON-TRIP PURPOSE,COUNT\n')
            for tour_purpose in sorted(tour_counts):
                w.write('{0},,{1}\n'.format(tour_purpose.upper(), tour_counts[tour_purpose]))
                for trip_purpose in sorted(trip_purposes):
                    w.write(',{0},{1}\n'.format(trip_purpose, trip_counts[tour_purpose].get(trip_purpose, 0)))

        return csv_path


    def export_trip_tod_csv(self, csv_path):
        ''' Export a CSV file containing the number of person-trips in each
            time-of-day period, by broad trip purpose. '''
        self.open_db()

        # Count person-trips by TOD and purpose
        trips = {i: {'H-W': 0, 'W-H': 0, 'H-O': 0, 'O-H': 0, 'NH': 0} for i in xrange(1,9)}
        trip_sql = (
            ''' SELECT Trips.purpose_o, Trips.purpose_d, Tours.participants, Trips.tod '''
            ''' FROM Trips LEFT JOIN Tours ON Trips.tour_id=Tours.tour_id ORDER BY Trips.trip_id '''
        )

        for purp_o, purp_d, participants, tod in self.query(trip_sql):
            n = self._unsample(len(participants.split()))
            if purp_d in ('work', 'school', 'university') and purp_o == 'home':
                purpose = 'H-W'
            elif purp_o in ('work', 'school', 'university') and purp_d == 'home':
                purpose = 'W-H'
            elif purp_o == 'home':
                purpose = 'H-O'
            elif purp_d == 'home':
                purpose = 'O-H'
            else:
                purpose = 'NH'
            trips[tod][purpose] += n

        self.close_db()

        # Write results to CSV
        with open(csv_path, 'wb') as w:
            w.write('TIME-OF-DAY,HOME-WORK,WORK-HOME,HOME-OTHER,OTHER-HOME,NON-HOME-BASED\n')
            for tod, trip_dict in sorted(trips.iteritems()):
                w.write('{0},{1},{2},{3},{4},{5}\n'.format(tod, trip_dict['H-W'], trip_dict['W-H'], trip_dict['H-O'], trip_dict['O-H'], trip_dict['NH']))

        return csv_path


    def export_vmt_statistics_csv(self, csv_path):
        ''' Export a CSV file containing VMT, stratified by zone group and
            facility type. '''
        # Initialize VMT dict
        vmt_subset = {g: {t: 0 for t in self.facility_types} for g in self.zone_groups}

        # Populate dict from Emme data
        emmebank = _eb.Emmebank(self._emmebank_path)

        for tod in xrange(1, 9):

            # Auto VMT from links in TOD's highway network
            scenario_id_hwy = '{0}'.format(tod)
            scenario_hwy = emmebank.scenario(scenario_id_hwy)
            network_hwy = scenario_hwy.get_network()

            for link in network_hwy.links():
                # Stratify by zone group
                zn = link.i_node['@zone']
                for g, z in self.zone_groups.iteritems():
                    if zn in z:
                        zone_group = g
                        break
                # Stratify by facility type
                for t, v in self.facility_types.iteritems():
                    if link.volume_delay_func in v:
                        facility_type = t
                        break

                # Calculate VMT
                vol = (  # Convert vehicle-equivalents to vehicles
                    link['@vso1n']/1 + link['@vso1t']/1 + link['@vho2n']/1 + link['@vho2t']/1 +
                    link['@vho3n']/1 + link['@vho3t']/1 + link['@vltrn']/1 + link['@vltrt']/1 +
                    link['@vmtrn']/2 + link['@vmtrt']/2 + link['@vhtrn']/3 + link['@vhtrt']/3
                )
                vmt = vol * link.length

                # Add VMT to appropriate group
                vmt_subset[zone_group][facility_type] += vmt

            # Bus VMT from transit segments in TOD's transit network
            scenario_id_trn = '10{0}'.format(tod)
            scenario_trn = emmebank.scenario(scenario_id_trn)
            network_trn = scenario_trn.get_network()

            for link in network_trn.links():
                # Stratify by zone group
                zn = link.i_node['@zone']
                for g, z in self.zone_groups.iteritems():
                    if zn in z:
                        zone_group = g
                        break
                # Stratify by facility type
                for t, v in self.facility_types.iteritems():
                    if link.volume_delay_func in v:
                        facility_type = v
                        break

                # Calculate headway- and TTF-adjusted VMT for each bus segment
                for tseg in link.segments():
                    if tseg.line.mode in ('B', 'E', 'L', 'P', 'Q'):

                        # Calculate line-specific volume from headway: must be at least 1; ignore headways of 99 mins)
                        vol = max(self.tod_minutes(tod) / tseg['@hdway'], 1) if tseg['@hdway'] != 99 else 1
                        vmt = vol * link.length

                        vmt_subset[zone_group][facility_type] += vmt

        emmebank.dispose()  # Close Emmebank, remove lock

        # Write results to CSV
        zngrp_order = ['Chicago', 'Cook Balance', 'DuPage', 'Kane', 'Kendall', 'Lake', 'McHenry', 'Will', 'IL Balance', 'Indiana', 'Wisconsin']
        factype_order = ['Expressway', 'Arterial', 'Ramp/Toll', 'Centroid']
        with open(csv_path, 'wb') as w:
            row = '{0},{1},{2}\n'
            w.write(row.format('DISTRICT', 'FACILITY_TYPE', 'VMT'))

            # Iterate through zone groups
            for zone_group in zngrp_order:
                for facility_type in factype_order:
                    vmt = vmt_subset[zone_group][facility_type]
                    w.write(row.format(zone_group, facility_type, vmt))
                vmt_subtotal = sum(v for v in vmt_subset[zone_group].itervalues())
                w.write(row.format(zone_group, 'Subtotal', vmt_subtotal))

            # Summarize entire network by facility type
            network_totals = {t: sum(vmt_subset[g][t] for g in self.zone_groups) for t in self.facility_types}
            for facility_type in factype_order:
                vmt = network_totals[facility_type]
                w.write(row.format('Entire Network', facility_type, vmt))
            grand_total = sum(v for v in network_totals.itervalues())
            w.write(row.format('Entire Network', 'Grand Total', grand_total))

            # Summarize CMAP 7-county region by facility type
            cmap_groups = ['Chicago', 'Cook Balance', 'McHenry', 'Lake', 'Kane', 'DuPage', 'Will', 'Kendall']
            cmap_totals = {t: sum(vmt_subset[g][t] for g in cmap_groups) for t in self.facility_types}
            for facility_type in factype_order:
                vmt = network_totals[facility_type]
                w.write(row.format('CMAP Region', facility_type, vmt))
            cmap_total = sum(v for v in cmap_totals.itervalues())
            w.write(row.format('CMAP Region', 'Region Total', cmap_total))

        return csv_path


    def _get_boardings(self, node_or_line, split_rail=False, scale_runs=True):
        ''' Sum transit segment boardings by inode (for stations) or
            tline_id (for vehicles). Return results in a dict. '''
        # Set GROUP BY column name for node or line
        if node_or_line == 'NODE':
            group = 'inode'
        elif node_or_line == 'LINE':
            group = 'tline_id'
        else:
            raise ValueError('Argument node_or_line must be "NODE" or "LINE".')

        # Construct dictionary from SQL query results
        if split_rail:
            boardings = {'RAIL': {}, 'BUS': {}}
        else:
            boardings = {}

        self.open_db()
        boarding_query = 'SELECT {0}, SUM(boardings), is_rail, tod, headway FROM TransitSegs WHERE allow_boardings=1 GROUP BY {0}'.format(group)
        for r in self.query(boarding_query):
            mode = 'RAIL' if r[2] else 'BUS'

            if scale_runs:
                # If 'LINE', scale multiple-run vehicles to average single-run boardings
                if node_or_line == 'NODE' or r[4] == 99:  # Metra and CTA have headway=99, but every run is modeled (REALLY?)
                    run_scaling = 1.0
                else:
                    run_scaling = min(1.0, r[4] / self.tod_minutes[r[3]])  # headway / minutes in TOD period (ceiling of 1.0)
            else:
                run_scaling = 1.0

            # Add boardings to appropriate dictionary
            if split_rail:
                boardings[mode][r[0]] = r[1] * run_scaling
            else:
                boardings[r[0]] = r[1] * run_scaling
        self.close_db()

        return boardings


    def _get_boarding_groups(self, boardings, num_groups=6, fraction=0.5):
        ''' With a dictionary of station/vehicle boardings (generated by
            ABM._get_boardings() as input, return a dictionary with the same
            keys, but where the values are the groups in which the boardings
            fall. These groups will be created by taking successive fractions
            of the remaining boardings, in order, based on a specified
            fraction (e.g. group 1: first half of all boardings; group 2:
            first half of boardings not in group 1; etc.). '''
        # Order boardings
        ordered_boardings = sorted(boardings.itervalues())
        num_boardings = len(ordered_boardings)

        # Calculate interval endpoints for groups
        def find_group_endpoints(num_boardings, num_groups, fraction, group=1, endpoints=None):
            if not endpoints:
                endpoints = {}
            start_pos = max(endpoints.itervalues()) if endpoints else 0
            if group == num_groups:
                end_pos = num_boardings
            else:
                end_pos = int(round(((num_boardings - start_pos) * fraction) + start_pos))
            endpoints[group] = end_pos
            if group < num_groups and end_pos < num_boardings:
                return find_group_endpoints(num_boardings, num_groups, fraction, group+1, endpoints)
            else:
                return endpoints

        endpoints = find_group_endpoints(num_boardings, num_groups, fraction)
        upper_bounds = {k: ordered_boardings[v-1] for k, v in endpoints.iteritems()}

        # Assign nodes/lines to groups
        boarding_groups = {}
        for k, v in boardings.iteritems():
            for group in sorted(upper_bounds.keys()):
                if v <= upper_bounds[group]:
                    boarding_groups[k] = group
                    break

        return boarding_groups


    def _get_boarding_quantiles(self, boardings, num_quantiles):
        ''' With a dictionary of station/vehicle boardings (generated by
            ABM._get_boardings() as input, return a dictionary with the same
            keys, but where the values are the quantiles in which the boardings
            fall. '''
        # Count frequency of zero-boardings, check if > quantile size
        n_zero = sum((1 for v in boardings.itervalues() if v == 0))
        q_size = len(boardings) / num_quantiles
        special_zero = n_zero > q_size
        brd = boardings.copy() if special_zero else boardings

        if special_zero:
            num_quantiles -= 1  # Do the 0 quantile manually
            boardings_zero = {k: v for k, v in brd.iteritems() if v == 0}
            for k in boardings_zero:
                del brd[k]

        # Sort boarding dict keys/values by node/line id
        sorted_boarding_keys = sorted(brd.keys())
        sorted_boarding_vals = [brd[k] for k in sorted_boarding_keys]

        # Save corresponding quantiles (zero-based) in new list
        sorted_boarding_quantiles = list(pd.qcut(sorted_boarding_vals, num_quantiles).labels)

        # Zip key and quantile lists together into dict
        boarding_quantiles = dict(zip(sorted_boarding_keys, sorted_boarding_quantiles))

        # If 0's handled separately, renumber quantiles and reattach 0's
        if special_zero:
            for k in boarding_quantiles.keys():
                boarding_quantiles[k] += 1
            boarding_quantiles_zero = {k: 0 for k in boardings_zero.keys()}
            boarding_quantiles.update(boarding_quantiles_zero)

        return boarding_quantiles


    def _get_link_speeds(self):
        ''' Assign highway links a free-flow and congested speed (mph), based
            on the in-link in the case of toll links (vdf=7). Return a nested
            dict keyed by TOD and link id, with values being tuples of
            (free-speed, congested-speed). '''
        link_speeds = {}
        toll_link_speeds = {}
        emmebank = _eb.Emmebank(self._emmebank_path)
        for tod in xrange(1, 9):
            link_speeds[tod] = {}
            toll_link_speeds[tod] = {}
            scenario_id_hwy = '{0}'.format(tod)
            scenario_hwy = emmebank.scenario(scenario_id_hwy)
            network_hwy = scenario_hwy.get_network()

            # Get inode for toll links
            for link in network_hwy.links():
                if link.volume_delay_func == 7:
                    toll_link_speeds[tod][link.id] = None

            toll_link_inodes = {link_id.split('-')[0]: link_id for link_id in toll_link_speeds[tod].iterkeys()}

            # Calc speed for non-toll links, also assigning to toll links as appropriate
            for link in network_hwy.links():
                if link.volume_delay_func != 7:

                    # Calculate volumes)
                    vol = (  # Convert vehicle-equivalents to vehicles
                        link['@vso1n']/1 + link['@vso1t']/1 + link['@vho2n']/1 + link['@vho2t']/1 +
                        link['@vho3n']/1 + link['@vho3t']/1 + link['@vltrn']/1 + link['@vltrt']/1 +
                        link['@vmtrn']/2 + link['@vmtrt']/2 + link['@vhtrn']/3 + link['@vhtrt']/3
                    )

                    # Get link travel times (minutes) and free-flow/modeled speeds (mph)
                    fmph = link.length / (link['@ftime'] / 60) if link['@ftime'] else 0
                    mph = link.length / (link.auto_time / 60) if link.auto_time else 0

                    # Adjust arterial speeds
                    if link.volume_delay_func == 1:
                        cap = link.data2  # Capacity is batched in to ul2 during network building
                        mph = fmph / ((math.log(fmph) * 0.249) + 0.153 * (vol / (cap * 0.75))**3.98)

                    # Write speeds to appropriate dicts
                    link_speeds[tod][link.id] = (fmph, mph)
                    if link.j_node.id in toll_link_inodes:
                        toll_link_id = toll_link_inodes[link.j_node.id]
                        toll_link_speeds[tod][toll_link_id] = link_speeds[tod][link.id]

            # Merge toll TOD dict into non-toll TOD dict
            link_speeds[tod].update(toll_link_speeds[tod])

        emmebank.dispose()  # Close Emmebank, remove lock
        return link_speeds


    def _get_matrix_data(self, matrix, tod):
        ''' Return an Emme Matrix Data object for a specified matrix. '''
        emmebank = _eb.Emmebank(self._emmebank_path)
        matrix_data = emmebank.matrix(matrix).get_data(tod)  ### At Emme 4.2, can get as numpy array with Matrix.get_numpy_data (or something like that) ###
        emmebank.dispose()  # Close Emmebank, remove lock
        return matrix_data


    def _get_mode_share(self, table='Trips'):
        ''' Return the mode share of trips (or tours). '''
        table_rows = self._count_rows(table)
        mode_share = {}
        for mode in sorted(self.modes.keys()):
            mode_share[mode] = self._count_rows(table, 'mode={0}'.format(mode)) / table_rows
        return mode_share


    def _get_ptrips_by_class(self, stratify_by_field=None):
        ''' Return count of transit person-trips, split by user class (1-3). '''
        ptrips_dict_template = {1: 0.0, 2: 0.0, 3: 0.0}

        # Initialize query components
        sql_select = 'SELECT PersonTrips.uclass'
        sql_from = 'FROM PersonTrips'
        sql_where = 'WHERE PersonTrips.mode IN (9, 10, 11, 12)'

        if stratify_by_field:
            # Update query to accommodate stratification field
            sql_select += ', {0}'.format(stratify_by_field)
            if stratify_by_field.lower().startswith('trips.'):
                sql_from += ' LEFT JOIN Trips ON PersonTrips.trip_id=Trips.trip_id'
            elif stratify_by_field.lower().startswith('tours.'):
                sql_from += ' LEFT JOIN Tours ON PersonTrips.tour_id=Tours.tour_id'
            elif stratify_by_field.lower().startswith('households.'):
                sql_from += ' LEFT JOIN Households ON PersonTrips.hh_id=Households.hh_id'
            elif stratify_by_field.lower().startswith('people.'):
                sql_from += ' LEFT JOIN People ON PersonTrips.pers_id=People.pers_id'
            elif stratify_by_field.lower().startswith('persontours.'):
                sql_from += ' LEFT JOIN PersonTours ON PersonTrips.ptour_id=PersonTours.ptour_id'

            # Identify unique stratification values to group output by
            table, field = stratify_by_field.split('.')
            sql_groups = 'SELECT DISTINCT {0} FROM {1}'.format(field, table)
            groups = [r[0] for r in self.query(sql_groups)]
        else:
            groups = [None]

        # Build final query
        sql = ' '.join((sql_select, sql_from, sql_where))

        # Initialize user class dict for each group
        ptrips_by_class = {}
        for group in groups:
            ptrips_by_class[group] = ptrips_dict_template.copy()

        for r in self.query(sql):
            uclass = r[0]

            # Get group ID
            if stratify_by_field:
                group = r[1]
            else:
                group = None

            # Add trip to appropriate group-user class combo
            ptrips_by_class[group][uclass] += self._unsample(1.0)

        if stratify_by_field:
            return ptrips_by_class
        else:
            return ptrips_by_class[None]


    def _get_vmt_by_speed(self):
        ''' Sum daily VMT by vehicle speed within the CMAP region, using 5 mph
            bins. For each TOD, process highway network first, followed by
            buses in corresponding transit network. '''
        vmt_by_speed = {i*5: 0 for i in xrange(15)}  # 15 5-mph bins, keyed by minimum speed
        link_speeds = self._get_link_speeds()
        emmebank = _eb.Emmebank(self._emmebank_path)

        for tod in xrange(1, 9):

            # Auto VMT from links in TOD's highway network
            scenario_id_hwy = '{0}'.format(tod)
            scenario_hwy = emmebank.scenario(scenario_id_hwy)
            network_hwy = scenario_hwy.get_network()

            for link in network_hwy.links():
                if 1 <= link.i_node['@zone'] <= 1711:

                    # Calculate VMT
                    vol = (  # Convert vehicle-equivalents to vehicles
                        link['@vso1n']/1 + link['@vso1t']/1 + link['@vho2n']/1 + link['@vho2t']/1 +
                        link['@vho3n']/1 + link['@vho3t']/1 + link['@vltrn']/1 + link['@vltrt']/1 +
                        link['@vmtrn']/2 + link['@vmtrt']/2 + link['@vhtrn']/3 + link['@vhtrt']/3
                    )
                    vmt = vol * link.length

                    # Get link travel times (minutes) and free-flow/modeled speeds (mph)
                    fmph, mph = link_speeds[tod][link.id]

                    # Add VMT to appropriate speed bin
                    mph_bin = 5 * min(int(math.floor(mph / 5)), 14)
                    vmt_by_speed[mph_bin] += vmt

            # Bus VMT from transit segments in TOD's transit network
            scenario_id_trn = '10{0}'.format(tod)
            scenario_trn = emmebank.scenario(scenario_id_trn)
            network_trn = scenario_trn.get_network()

            for link in network_trn.links():
                if 1 <= link.i_node['@zone'] <= 1711:

                    # Calculate headway- and TTF-adjusted VMT for each bus segment
                    for tseg in link.segments():
                        if tseg.line.mode in ('B', 'E', 'L', 'P', 'Q'):

                            # Calculate line-specific volume from headway: must be at least 1; ignore headways of 99 mins)
                            vol = max(self.tod_minutes(tod) / tseg['@hdway'], 1) if tseg['@hdway'] != 99 else 1
                            vmt = vol * link.length

                            # Get link travel times (minutes) and free-flow/modeled speeds (mph)
                            fmph, mph = link_speeds[tod][link.id]

                            # Add VMT to appropriate speed bin
                            if tseg.transit_time_func == 2:
                                mph_bin = 5 * min(int(math.floor(fmph / 5)), 14)
                            else:
                                mph_bin = 5 * min(int(math.floor(mph / 5)), 14)
                            vmt_by_speed[mph_bin] += vmt

        emmebank.dispose()  # Close Emmebank, remove lock
        return vmt_by_speed


    def _guess_transit_ptrips_modes(self):
        ''' Approximate the number of trips for each transit mode, based on '''
        ''' the modeshare of tlines serving origin TAPs. '''
        sql_select = 'SELECT Trips.tap_o'
        sql_from = 'FROM PersonTrips LEFT JOIN Trips ON PersonTrips.trip_id=Trips.trip_id'
        sql_where = 'WHERE Trips.tap_o > 0'
        sql = ' '.join((sql_select, sql_from, sql_where))
        mode_trips = {mode: 0.0 for mode in self.transit_modes.iterkeys()}
        self.open_db()
        for r in self.query(sql):
            tap = r[0]
            for mode, share in self.tap_modes[tap].iteritems():
                mode_trips[mode] += self._unsample(share)
        self.close_db()
        return mode_trips


    def _insert_households(self, hh_csv):
        ''' Populate the Households table from a CSV. '''
        with open(hh_csv, 'rb') as csvfile:
            r = csv.DictReader(csvfile)
            for d in r:

                # Get values
                hh_id = int(d['hh_id'])
                sz = int(d['maz'])
                size = int(d['size'])

                # Insert into table
                db_row = (hh_id, sz, size)
                insert_sql = 'INSERT INTO Households VALUES ({0})'.format(','.join(['?'] * len(db_row)))
                self._con.execute(insert_sql, db_row)

        self._con.commit()
        return None


    def _insert_people(self, pers_csv):
        ''' Populate the People table from a CSV. '''
        with open(pers_csv, 'rb') as csvfile:
            r = csv.DictReader(csvfile)
            for d in r:
                # Get values
                hh_id = int(d['hh_id'])
                pers_num = int(d['person_num'])
                pers_id = '{0}-{1}'.format(hh_id, pers_num)  # NOTE: NOT the 'person_id' value from CSV
                age = int(d['age'])
                gender = self._clean_str(d['gender'])
                uc_w_w = int(d['user_class_work_walk'])
                uc_w_p = int(d['user_class_work_pnr'])
                uc_w_k = int(d['user_class_work_knr'])
                uc_o_w = int(d['user_class_non_work_walk'])
                uc_o_p = int(d['user_class_non_work_pnr'])
                uc_o_k = int(d['user_class_non_work_knr'])

                # Insert into table
                db_row = (pers_id, hh_id, pers_num, age, gender, uc_w_w, uc_w_p, uc_w_k, uc_o_w, uc_o_p, uc_o_k)
                insert_sql = 'INSERT INTO People VALUES ({0})'.format(','.join(['?'] * len(db_row)))
                self._con.execute(insert_sql, db_row)

        self._con.commit()
        return None


    def _insert_tours(self, tours_csv, is_joint):
        ''' Populate the Tours and PersonTours tables from a CSV. '''
        with open(tours_csv, 'rb') as csvfile:
            r = csv.DictReader(csvfile)
            for d in r:
                # Get values
                hh_id = int(d['hh_id'])
                participants = str(d['tour_participants']) if is_joint else str(d['person_num'])
                pers_num = 'J' if is_joint else participants
                tour_num = int(d['tour_id'])
                purpose = self._clean_str(str(d['tour_purpose']))
                category = self._clean_str(str(d['tour_category']))
                sz_o = int(d['orig_maz'])
                sz_d = int(d['dest_maz'])
                tod_d = self._convert_time_period(int(d['depart_period']))
                tod_a = self._convert_time_period(int(d['arrive_period']))
                mode = int(d['tour_mode'])
                tour_id = '{0}-{1}-{2}-{3}'.format(hh_id, pers_num, tour_num, purpose)

                # Insert into table
                db_row = (
                    tour_id, hh_id, participants, pers_num, is_joint, category,
                    purpose, sz_o, sz_d, tod_d, tod_a, mode
                )
                insert_sql = 'INSERT INTO Tours VALUES ({0})'.format(','.join(['?'] * len(db_row)))
                self._con.execute(insert_sql, db_row)

                # Split tours into person-tours
                for participant in participants.strip().split():
                    pers_id = '{0}-{1}'.format(hh_id, participant)
                    ptour_id = '{0}-{1}'.format(tour_id, participant)
                    # Insert into table
                    db_row = (
                        ptour_id, tour_id, hh_id, pers_id, mode
                    )
                    insert_sql = 'INSERT INTO PersonTours VALUES ({0})'.format(','.join(['?'] * len(db_row)))
                    self._con.execute(insert_sql, db_row)

        self._con.commit()
        return None


    def _insert_trips(self, trips_csv, is_joint):
        ''' Populate the Trips and PersonTrips tables from a CSV and the People & Tours tables. '''

        # ### DEBUG ###
        # import psutil
        # process = psutil.Process(os.getpid())
        # print str(process.memory_info().rss / 1024.0**3) + ' GB memory used'
        # ### /DEBUG ###

        # Get people user-classes and tour categories for setting person-trip user-class.
        # (Shelve these dicts to free up a ton of memory.)
        people_uclasses_dict = {str(r[0]): list(r)[1:] for r in self.query("SELECT pers_id, class_w_wtt, class_w_pnr, class_w_knr, class_o_wtt, class_o_pnr, class_o_knr FROM People")}
        people_uclasses_path = os.path.join(self._TEST_DIR, 'people_uclasses.shelve')
        people_uclasses = shelve.open(people_uclasses_path)
        people_uclasses.update(people_uclasses_dict)
        del people_uclasses_dict
        # print str(process.memory_info().rss / 1024.0**3) + ' GB memory used'  ### DEBUG ###

        tour_categories_dict = {str(r[0]): r[1] for r in self.query("SELECT tour_id, category FROM Tours")}
        tour_categories_path = os.path.join(self._TEST_DIR, 'tour_categories.shelve')
        tour_categories = shelve.open(tour_categories_path)
        tour_categories.update(tour_categories_dict)
        del tour_categories_dict
        # print str(process.memory_info().rss / 1024.0**3) + ' GB memory used'  ### DEBUG ###

        # Load CSV into a pandas dataframe for easy slicing/querying
        chunked_trips = pd.read_csv(trips_csv, iterator=True, chunksize=1000000)  # Chunk to avoid MemoryError

        # Process chunk of trips by matrix mode
        for trips in chunked_trips:
            # print str(process.memory_info().rss / 1024.0**3) + ' GB memory used'  ### DEBUG ###
            for matrix_mode in xrange(7):
                # Filter trips by mode
                if matrix_mode == 0:  # Walk, bike, walk-to-transit
                    trip_modes = [7, 8, 9, 10]
                elif matrix_mode == 1:  # Drive alone, free
                    trip_modes = [1, 11, 12, 13, 14]  # Include drive-to-transit, taxis, school buses
                else:  # Other driving modes
                    trip_modes = [matrix_mode]
                mode_trips = trips[trips.trip_mode.isin(trip_modes)]

                # Filter trips further by time-of-day
                for tod in xrange(1, 9):

                    ctramp_periods = self._convert_time_period(tod, ctramp_to_emme=False)
                    subset_trips = mode_trips[mode_trips.stop_period.isin(ctramp_periods)]
                    matrix_ids = {}
                    matrix_data = {}
                    # if matrix_mode > 0:
                    #     matrix_ids['t'] = 'mf{0}{1}'.format(tod, self._get_matrix_nums(matrix_mode)[0])
                    #     matrix_ids['d'] = 'mf{0}{1}'.format(tod, self._get_matrix_nums(matrix_mode)[1])
                    #     matrix_data['t'] = self._get_matrix_data(matrix_ids['t'], tod)
                    #     matrix_data['d'] = self._get_matrix_data(matrix_ids['d'], tod)

                    # Copy data into database
                    for index, row in subset_trips.iterrows():
                        # Get values
                        hh_id = int(row['hh_id'])
                        pers_num = 'J' if is_joint else str(row['person_num'])
                        tour_num = int(row['tour_id'])
                        purpose_t = self._clean_str(str(row['tour_purpose']))
                        inbound = int(row['inbound'])
                        stop_id = int(row['stop_id']) + 1  # to avoid all the -1's
                        purpose_o = self._clean_str(str(row['orig_purpose']))
                        purpose_d = self._clean_str(str(row['dest_purpose']))
                        sz_o = int(row['orig_maz'])
                        sz_d = int(row['dest_maz'])
                        zn_o = int(row['orig_taz'])
                        zn_d = int(row['dest_taz'])
                        tap_o = int(row['board_tap'])
                        tap_d = int(row['alight_tap'])
                        tod = self._convert_time_period(int(row['stop_period']))
                        mode = int(row['trip_mode'])
                        tour_id = '{0}-{1}-{2}-{3}'.format(hh_id, pers_num, tour_num, purpose_t)
                        trip_id = '{0}-{1}-{2}'.format(tour_id, inbound, stop_id)

                        # Estimate DRIVE time, distance, speed
                        speed = time = distance = 0  ### DEBUG ###
                        # if mode <= 6 or mode >= 13:  # Private autos, incl. taxis, school buses
                        #     time = matrix_data['t'].get(zn_o, zn_d)
                        #     distance = matrix_data['d'].get(zn_o, zn_d)
                        # elif mode in (11, 12):  # Drive-to-transit (assume drive alone, free)
                        #     from_zone = self.tap_zones[tap_d] if inbound else zn_o
                        #     to_zone = zn_d if inbound else self.tap_zones[tap_o]
                        #     time = matrix_data['t'].get(from_zone, to_zone)
                        #     distance = matrix_data['d'].get(from_zone, to_zone)
                        # else:  # Walk, bike, walk-to-transit
                        #     time = 0
                        #     distance = 0
                        # speed = distance / (time / 60) if (time and distance) else 0

                        # Insert into table
                        db_row = (
                            trip_id, tour_id, hh_id, pers_num, is_joint, inbound,
                            purpose_o, purpose_d, sz_o, sz_d, zn_o, zn_d, tap_o, tap_d,
                            tod, mode, time, distance, speed
                        )
                        insert_sql = 'INSERT INTO Trips VALUES ({0})'.format(','.join(['?'] * len(db_row)))
                        self._con.execute(insert_sql, db_row)

                        # Split trips into person-trips
                        tour_participants = [r[0] for r in self.query("SELECT participants FROM Tours WHERE tour_id = '{0}'".format(tour_id))][0]
                        for participant in tour_participants.strip().split():
                            # Get values
                            pers_id = '{0}-{1}'.format(hh_id, participant)
                            ptour_id = '{0}-{1}'.format(tour_id, participant)
                            ptrip_id = '{0}-{1}'.format(trip_id, participant)

                            # Assign each person-trip the appropriate user-class,
                            # based on trip mode and category (mandatory or not).
                            ptrip_category = tour_categories[tour_id]
                            if ptrip_category == 'mandatory':  # Use "work" user classes
                                wtt = people_uclasses[pers_id][0]
                                pnr = people_uclasses[pers_id][1]
                                knr = people_uclasses[pers_id][2]
                            else:  # Use "non-work" user classes
                                wtt = people_uclasses[pers_id][3]
                                pnr = people_uclasses[pers_id][4]
                                knr = people_uclasses[pers_id][5]

                            if mode in (9, 10):
                                uclass = wtt
                            elif mode in (11, 12):
                                uclass = max(pnr, knr)  # Assume drive-to-transit users prefer premium service
                                #uclass = (pnr, knr)[hh_id % 2]  # Assume 50/50 split between PNR & KNR trips (random, but deterministic)
                            else:
                                uclass = None

                            # Insert into table
                            db_row = (
                                ptrip_id, ptour_id, trip_id, tour_id, hh_id, pers_id, mode, uclass
                            )
                            insert_sql = 'INSERT INTO PersonTrips VALUES ({0})'.format(','.join(['?'] * len(db_row)))
                            self._con.execute(insert_sql, db_row)

                    self._con.commit()
                    del subset_trips, matrix_ids, matrix_data

                del mode_trips

            del trips

        people_uclasses.close()
        tour_categories.close()
        os.remove(people_uclasses_path)
        os.remove(tour_categories_path)

        return None


    def _insert_tsegs(self):
        ''' Populate the TransitSegs table from Emme transit assignments. '''
        emmebank = _eb.Emmebank(self._emmebank_path)
        for tod in xrange(1, 9):
            scenario_id = '10{0}'.format(tod)
            scenario = emmebank.scenario(scenario_id)
            network = scenario.get_network()
            for tseg in network.transit_segments():
                # Get values
                inode = tseg.i_node
                jnode = tseg.j_node
                if inode and jnode:
                    link = tseg.link
                    tline = tseg.line
                    tline_desc = tline.description  # Should this be trimmed? Combined with mode (tline[0])?
                    is_rail = True if tline.mode.id.upper() in ('C', 'M') else False
                    boardings = tseg.transit_boardings
                    allow_brd = tseg.allow_boardings
                    passengers = tseg.transit_volume
                    pass_hrs = passengers * tseg.transit_time / 60.0
                    pass_mi = passengers * link.length

                    # Insert into table (if valid link)
                    db_row = (
                        tseg.id, tline.id, tline_desc, tline.headway, tseg.number,
                        inode.number, jnode.number, tod, tline.mode.id, is_rail,
                        boardings, allow_brd, passengers, pass_hrs, pass_mi
                    )
                    insert_sql = 'INSERT INTO TransitSegs VALUES ({0})'.format(','.join(['?'] * len(db_row)))
                    self._con.execute(insert_sql, db_row)

            self._con.commit()

        emmebank.dispose()  # Close Emmebank, remove lock
        return None


    def open_db(self):
        ''' Open the database connection. '''
        self._con = sqlite3.connect(self._db)
        self._con.row_factory = sqlite3.Row
        return None


    def print_mode_share(self, grouped=True):
        ''' Print the mode share of trips. '''
        print ' '
        if grouped:
            mode_share_grouped = {
                'Auto (Excl. Taxi)': sum(self.mode_share[m] for m in xrange(1, 7)),
                'Drive-to-Transit': sum(self.mode_share[m] for m in (11, 12)),
                'Walk-to-Transit': sum(self.mode_share[m] for m in (9, 10)),
                'Walk/Bike/Taxi/School Bus': sum(self.mode_share[m] for m in (7, 8, 13, 14))
            }
            print 'MODE SHARE (GROUPED)'
            print '--------------------'
            for mode in sorted(mode_share_grouped.keys()):
                print '{0:<25}{1:>10.2%}'.format(mode, mode_share_grouped[mode])
        else:
            print 'MODE SHARE'
            print '----------'
            for mode in sorted(self.modes.keys()):
                print '{0:<25}{1:>10.2%}'.format(self._get_mode_str(mode), self.mode_share[mode])
        print ' '
        return None


    def print_ptrips_by_class(self):
        ''' Print the number and percentage of transit person-trips, stratified
            by user class. '''
        print ' '
        print 'LINKED TRANSIT PERSON-TRIPS BY USER CLASS'
        print '-----------------------------------------'
        total_ptrips = sum(self.ptrips_by_class.itervalues())
        for uclass in sorted(self.ptrips_by_class.keys()):
            ptrips = self.ptrips_by_class[uclass]
            ptrips_pct = ptrips / total_ptrips
            print '{0:<25}{1:>10,.0f} ({2:.2%})'.format('User Class {0}'.format(uclass), ptrips, ptrips_pct)
        print '{0:<25}{1:>10,.0f}'.format('All User Classes', total_ptrips)
        print ' '
        return None


    def print_transit_stats(self, grouped=True):
        ''' Print the boardings, passenger miles traveled and passenger hours
            traveled, by mode or grouped. '''
        print ' '
        if grouped:
            total_boardings = sum(self.transit_stats['BOARDINGS'].itervalues())
            total_pmt = sum(self.transit_stats['PMT'].itervalues())
            total_pht = sum(self.transit_stats['PHT'].itervalues())
            print 'TRANSIT STATS'
            print '-------------'
            print ' {0:<15} | {1:<15} | {2:<15} '.format('Boardings', 'Pass. Miles', 'Pass. Hours')
            print '{0:-<17}|{0:-<17}|{0:-<17}'.format('')
            print ' {0:>15,.0f} | {1:>15,.0f} | {2:>15,.0f} '.format(total_boardings, total_pmt, total_pht)
        else:
            print 'TRANSIT STATS BY MODE'
            print '---------------------'
            print ' {0:<20} | {1:<15} | {2:<15} | {3:<15} '.format('Mode', 'Boardings', 'Pass. Miles', 'Pass. Hours')
            print '{0:-<22}|{0:-<17}|{0:-<17}|{0:-<17}'.format('')
            for mode_code, mode_desc in sorted(self.transit_modes.iteritems(), key=lambda (k, v): v):
                boardings = self.transit_stats['BOARDINGS'][mode_code]
                pmt = self.transit_stats['PMT'][mode_code]
                pht = self.transit_stats['PHT'][mode_code]
                print ' {0:<20} | {1:>15,.0f} | {2:>15,.0f} | {3:>15,.0f} '.format(mode_desc, boardings, pmt, pht)
        print ' '
        return None


    def print_vmt_by_speed(self):
        ''' Print the total daily VMT by 5-mph speed bin, including a crude
            histogram. '''
        print ' '
        print 'DAILY VMT BY SPEED (MPH)'
        print '------------------------'
        total_vmt = sum(self.vmt_by_speed.itervalues())
        for speed_bin in sorted(self.vmt_by_speed.keys()):
            vmt = self.vmt_by_speed[speed_bin]
            vmt_pct = vmt / total_vmt
            if speed_bin <= 65:
                bin_label = '{0}-{1}'.format(speed_bin, speed_bin+5)
            else:
                bin_label = '70+'
            numbers = '{0:<6}{1:>15,.2f} ({2:.2%})'.format(bin_label, vmt, vmt_pct)
            print '{0:<30}  {1}'.format(numbers, '|' * int(round(vmt_pct * 100)))
        print '{0:<6}{1:>15,.2f}'.format('Total', total_vmt)
        print ' '
        return None


    def query(self, sql_query):
        ''' Execute a SQL query and return the cursor object. '''
        return self._con.execute(sql_query)


    @property
    def transit_stats(self):
        ''' Return the boardings, passenger miles traveled and passenger hours
            traveled, by mode. '''
        transit_stats = {
            'BOARDINGS': {},
            'PMT': {},
            'PHT': {}
        }
        query = 'SELECT transit_mode, SUM(boardings), SUM(pass_mi), SUM(pass_hrs) FROM TransitSegs GROUP BY transit_mode'
        for r in self._con.execute(query):
            transit_mode, boardings, pass_mi, pass_hrs = r
            transit_stats['BOARDINGS'][transit_mode] = boardings
            transit_stats['PMT'][transit_mode] = pass_mi
            transit_stats['PHT'][transit_mode] = pass_hrs
        return transit_stats


    def _unsample(self, num, sample_rate=None):
        ''' Divide a number by sample rate to approximate 100% sample. '''
        if not sample_rate:
            sample_rate = self.sample_rate
        return num / sample_rate
