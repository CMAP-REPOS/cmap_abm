#!/usr/bin/env python
'''
    abm.py
    Author: npeterson
    Revised: 3/18/16
    ---------------------------------------------------------------------------
    A class for reading ABM output files and matrix data into an SQL database
    for querying and summarization.

'''
import os
import csv
import math
import shelve
import sqlite3
import pandas as pd
from collections import Counter
from inro.emme.database import emmebank as _eb


class ABM(object):
    ''' A class for loading ABM model run output data into a SQLite database.
        Initialized with path (parent directory of 'cmap_abm') and model run
        sample rate (default 0.50). '''

    # --- Generic ABM variables ---
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
        #9: 'N/A',              # formerly 'Walk to local transit'
        10: 'Walk to transit',  # formerly 'Walk to premium transit'
        11: 'Kiss-n-ride',      # formerly 'Drive to local transit'
        12: 'Park-n-ride',      # formerly 'Drive to premium transit'
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
        'Chicago':      set(xrange(   1,  310)),  # xrange upper-bounds are *exclusive*
        'Cook Balance': set(xrange( 310,  855)),
        'McHenry':      set(xrange( 855,  959)),
        'Lake':         set(xrange( 959, 1134)),
        'Kane':         set(xrange(1134, 1279)),
        'DuPage':       set(xrange(1279, 1503)),
        'Will':         set(xrange(1503, 1691)),
        'Kendall':      set(xrange(1691, 1712)),
        'IL Balance':   set(xrange(1712, 1836)),  # 1712 & 1752 are in CMAP MPO boundary
        'Indiana':      set(xrange(1836, 1910)),
        'Wisconsin':    set(xrange(1910, 1945))
    }


    # --- Init ---
    def __init__(self, abm_dir, sample_rate=0.50, build_db=False):
        self.dir = abm_dir
        self.sample_rate = sample_rate
        self.name = os.path.basename(self.dir)
        self._input_dir = os.path.join(self.dir, 'cmap_abm', 'inputs')
        self._output_dir = os.path.join(self.dir, 'cmap_abm', 'outputs')
        self._emmebank_path = os.path.join(self.dir, 'cmap_abm', 'CMAP-ABM', 'Database', 'emmebank')
        self._db = os.path.join(self._output_dir, 'results.sqlite')
        #self._db = r'D:\workspace\Temp\ABM\results_{0}.sqlite'.format(self.name)  ### DEBUG ###
        if build_db and os.path.exists(self._db):
            print 'Reinitializing existing results database...'.format(self._db)
            os.remove(self._db)
        if not build_db and not os.path.exists(self._db):
            #raise ValueError('SQLite database {0} does not yet exist. Please set build_db=True.'.format(self._db))
            build_db = True  # Force if not yet built
            print 'Initializing new results database...'.format(self._db)

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

        # Create DB to store CT-RAMP output (or open existing)
        print 'Opening database ({0})...'.format(self._db)
        self._open_db()

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
        self.transit_stats = self._get_transit_stats()
        self.vmt_by_speed = self._get_vmt_by_speed()

        self._close_db()

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
    def _get_mode_str(cls, mode_num):
        ''' Return description of a mode code. '''
        return cls.modes[mode_num]


    # --- Instance methods ---
    def _clean_str(self, string):
        ''' Clean a string for database entry. '''
        return string.lower().replace('-', '').replace(' ', '')


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

        boarding_query = 'SELECT {0}, SUM(boardings), is_rail, tod, headway FROM TransitSegs WHERE allow_boardings=1 GROUP BY {0}'.format(group)
        for r in self.query(boarding_query):
            mode = 'RAIL' if r[2] else 'BUS'

            if scale_runs:
                # If 'LINE', scale multiple-run vehicles to average single-run boardings
                if node_or_line == 'NODE' or r[4] == 99:  # Pre-C16Q2, Metra and CTA have headway=99, but every run is modeled
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

        return boardings


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
        sql_where = 'WHERE PersonTrips.mode IN (10, 11, 12)'

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


    def _get_transit_stats(self):
        ''' Return the boardings, passenger miles traveled and passenger hours
            traveled, by mode. '''
        transit_stats = {
            'BOARDINGS': {},
            'PMT': {},
            'PHT': {}
        }
        sql_query = 'SELECT transit_mode, SUM(boardings), SUM(pass_mi), SUM(pass_hrs) FROM TransitSegs GROUP BY transit_mode'
        for r in self.query(sql_query):
            transit_mode, boardings, pass_mi, pass_hrs = r
            transit_stats['BOARDINGS'][transit_mode] = boardings
            transit_stats['PMT'][transit_mode] = pass_mi
            transit_stats['PHT'][transit_mode] = pass_hrs
        return transit_stats


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
        people_uclasses_path = os.path.join(self._output_dir, 'people_uclasses.shelve')
        people_uclasses = shelve.open(people_uclasses_path)
        people_uclasses.update(people_uclasses_dict)
        del people_uclasses_dict
        # print str(process.memory_info().rss / 1024.0**3) + ' GB memory used'  ### DEBUG ###

        tour_categories_dict = {str(r[0]): r[1] for r in self.query("SELECT tour_id, category FROM Tours")}
        tour_categories_path = os.path.join(self._output_dir, 'tour_categories.shelve')
        tour_categories = shelve.open(tour_categories_path)
        tour_categories.update(tour_categories_dict)
        del tour_categories_dict
        # print str(process.memory_info().rss / 1024.0**3) + ' GB memory used'  ### DEBUG ###

        # Load CSV into a pandas dataframe for easy slicing/querying
        chunked_trips = pd.read_csv(trips_csv, iterator=True, chunksize=1000000)  # Chunk to avoid MemoryError

        # Process chunk of trips by matrix mode
        for trips in chunked_trips:
            # print str(process.memory_info().rss / 1024.0**3) + ' GB memory used'  ### DEBUG ###

            # Copy data into database
            for index, row in trips.iterrows():
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

                # Insert into table
                db_row = (
                    trip_id, tour_id, hh_id, pers_num, is_joint,
                    inbound, purpose_o, purpose_d, sz_o, sz_d,
                    zn_o, zn_d, tap_o, tap_d, tod, mode
                )
                insert_sql = 'INSERT INTO Trips VALUES ({0})'.format(','.join(['?'] * len(db_row)))
                self._con.execute(insert_sql, db_row)

            self._con.commit()
            del trips

        # Create person-trips from trips
        sql_ptrips = (
            'SELECT Trips.trip_id, Trips.tour_id, Trips.hh_id, Trips.mode,'
            ' Tours.participants'
            ' FROM Trips LEFT JOIN Tours ON Trips.tour_id=Tours.tour_id'
            ' WHERE Trips.is_joint = {0}'
        ).format(int(is_joint))

        for r in self.query(sql_ptrips):
            trip_id = str(r[0])
            tour_id = str(r[1])
            hh_id = int(r[2])
            mode = int(r[3])
            tour_participants = r[4].strip().split()

            for participant in tour_participants:
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

                uclass = {10: wtt, 11: knr, 12: pnr}.get(mode, None)

                # Insert into table
                db_row = (
                    ptrip_id, ptour_id, trip_id, tour_id, hh_id, pers_id, mode, uclass
                )
                insert_sql = 'INSERT INTO PersonTrips VALUES ({0})'.format(','.join(['?'] * len(db_row)))
                self._con.execute(insert_sql, db_row)

        self._con.commit()

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


    def _unsample(self, num, sample_rate=None):
        ''' Divide a number by sample rate to approximate 100% sample. '''
        if not sample_rate:
            sample_rate = self.sample_rate
        return num / sample_rate


    def _db_is_closed(self):
        ''' Check whether connection to SQLite DB is closed. '''
        try:
            self._con.execute('PRAGMA user_version')  # Quick, schema-independent query
            return False  # Query worked => DB is *not* closed
        except sqlite3.ProgrammingError:
            return True

    def _db_is_open(self):
        ''' Check whether connection to SQLite DB is open. '''
        return not self._db_is_closed()


    def _open_db(self):
        ''' Open the database connection. '''
        self._con = sqlite3.connect(self._db)
        self._con.row_factory = sqlite3.Row
        return None


    def _close_db(self):
        ''' Close the database connection. '''
        self._con.close()
        return None


    def _count_rows(self, table, where_clause=None):
        ''' Execute a SELECT COUNT(*) query on a table with optional where
            clause and return the integer result. '''
        db_closed = self._db_is_closed()
        if db_closed:
            self._open_db()

        sql_query = 'SELECT COUNT(*) FROM {0}'.format(table)
        if where_clause:
            sql_query += ' WHERE {0}'.format(where_clause)
        count = float(self._con.execute(sql_query).fetchone()[0])

        if db_closed:
            self._close_db()
        return count


    def query(self, sql_query):
        ''' Execute a SQL query and return the cursor object. '''
        db_closed = self._db_is_closed()
        if db_closed:
            self._open_db()

        for row in self._con.execute(sql_query):
            yield row

        if db_closed:
            self._close_db()
