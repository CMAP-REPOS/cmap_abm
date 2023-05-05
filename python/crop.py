# crop CMAP data to a smaller geography
# Originally from marin tvpb example data processing, Ben Stabler, ben.stabler@rsginc.com, 09/17/20

import os
import pandas as pd
import openmatrix as omx
import argparse
import numpy as np

MAZ_OFFSET = -499

MAZ_LIST = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 396, 397, 398, 399, 400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 431, 432, 433, 434, 435, 436, 437, 438, 439, 440, 441, 442, 443, 444, 445, 446, 447, 448, 449, 450, 451, 452, 453, 454, 455, 456, 457, 458, 459, 460, 461, 462, 463, 464, 465, 466, 467, 468, 469, 470, 471, 472, 473, 474, 475, 476, 477, 478, 479, 480, 481, 482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495, 496, 497, 498, 499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511, 512, 513, 514, 515, 516, 517, 518, 519, 520, 521, 522, 523, 524, 525, 526, 527, 528, 529, 530, 531, 532, 533, 534, 535, 536, 537, 538, 539, 540, 541, 542, 543, 568, 569, 570, 571, 572, 573, 582, 583, 584, 585, 586, 587, 588, 589, 590, 591, 592, 593, 594, 595, 596, 597, 598, 599, 600, 601, 602, 603, 604, 605, 606, 607, 608, 609, 610, 611, 612, 613, 614, 615, 616, 617, 618, 619, 620, 621, 622, 623, 624, 625, 626, 627, 628, 629, 630, 631, 632, 633, 634, 635, 636, 637, 638, 639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 649, 650, 651, 652, 653, 654, 655, 656, 657, 658, 659, 660, 661, 662, 663, 664, 665, 666, 667, 668, 669, 670, 671, 672, 673, 674, 675, 676, 677, 678, 679, 680, 681, 682, 683, 684, 685, 686, 687, 688, 689, 690, 691, 692, 693, 694, 695, 696, 715, 718, 719, 720, 721, 722, 723, 724, 725, 726, 727, 728, 729, 730, 738, 739, 740, 741, 742, 743, 744, 745, 746, 747, 748, 749, 750, 751, 752, 753, 754, 755, 756, 757, 758, 759, 760, 761, 762, 763, 764, 765, 766, 767, 768, 769, 770, 771, 772, 773, 774, 775, 776, 777, 778, 779, 780, 781, 782, 783, 784, 785, 786, 787, 788, 789, 790, 791, 792, 793, 794, 795, 796, 797, 798, 799, 800, 801, 802, 803, 804, 805, 806, 807, 808, 809, 810, 811, 812, 813, 814, 815, 816, 817, 818, 819, 820, 821, 822, 823, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833, 834, 835, 836, 837, 838, 839, 840, 841, 842, 843, 844, 845, 846, 847, 848, 849, 850, 851, 852, 853, 854, 855, 856, 857, 858, 859, 860, 861, 862, 863, 864, 865, 866, 867, 868, 869, 870, 871, 872, 873, 874, 875, 876, 877, 878, 879, 880, 881, 882, 883, 884, 885, 886, 887, 888, 889, 890, 891, 892, 893, 894, 895, 896, 897, 898, 899, 900, 901, 902, 903, 904, 905, 906, 907, 908, 909, 910, 911, 912, 913, 914, 915, 916, 917, 918, 919, 920, 921, 922, 923, 924, 925, 926, 927, 928, 929, 930, 931, 932, 933, 934, 935, 936, 937, 938, 939, 940, 941, 945, 946, 954, 955, 956, 957, 958, 959, 960, 961, 962, 963, 964, 965, 966, 967, 968, 969, 970, 971, 972, 973, 974, 975, 976, 977, 978, 979, 980, 981, 982, 983, 2175, 2176, 2177, 2178, 2179, 2180, 2181, 2182, 2183, 2184, 2185, 2186, 2187, 2188, 2189, 2190, 2191, 2192, 2193, 2194, 2195, 2196, 2197, 2199, 2201, 2203, 2204, 2205, 2206, 2207, 2208, 2209, 2210, 2211, 2212, 2213, 2214, 2215, 2216, 2217, 2218, 2219, 2220, 2221, 2222, 2223, 2224, 2225, 2226, 2227, 2228, 2229, 2230, 2231, 2232, 2233, 2234, 2235, 2236, 2237, 2238, 2239, 2240, 2241, 2242, 2243, 2245, 2247, 2251, 2252, 2253, 2254, 2255, 2256, 2257, 2258, 2259, 2260, 2261, 2262, 2263, 2264, 2265, 2266, 2267, 2268, 2269, 2270, 2271, 2272, 2273, 2274, 2275, 2276, 2277, 2278, 2279, 2280, 2281, 2282, 2283, 2284, 2285, 2287, 2319, 2320, 2321, 2322, 2323, 2324, 2325, 2326, 2327, 2328, 2329, 2330, 2331, 2332, 2333, 2334, 2335, 2336, 2337, 2338, 2339, 2340, 2341, 2342, 2343, 2344, 2345, 2346, 2347, 2348, 2349, 2350, 2351, 2352, 2365, 2366, 2367, 2368, 2369, 2370, 2371, 2372, 2373, 2374, 2375, 2376, 2377, 2378, 2379, 2380, 2381, 2382, 2383, 2384, 2385, 2386, 2387, 2388, 2389, 2390, 2391, 2392, 2393, 2394, 2395, 2396, 2397, 2398, 2399, 2400, 2401, 2402, 2403, 2404, 2405, 2406, 2407, 2408, 2409, 2410, 2411, 2412, 2413, 2414, 2415, 2416, 2417, 2418, 2419, 2420, 2421, 2422, 2423, 2424, 2425, 2426, 2427, 2428, 2429, 2430, 2431, 2432, 2433, 2434, 2435, 2436, 2437, 2438, 2439, 2440, 2441, 2442, 2443, 2444, 2445, 2446, 2447, 2448, 2449, 2450, 2451, 2452, 2453, 2454, 2455, 2456, 2457, 2458, 2459, 2460, 2461, 2462, 2463, 2464, 2465, 2466, 2467, 2468, 2469, 2470, 2471, 2472, 2473, 2474, 2475, 2476, 2477, 2478, 2479, 2480, 2481, 2482, 2483, 2484, 2485, 2486, 2487, 2488, 2489, 2490, 2491, 2492, 2493, 2494, 2495, 2496, 2497, 2498, 2499, 2500, 2501, 2502, 2503, 2504, 2505, 2506, 2507, 2508, 2509, 2510, 2511, 2512, 2513, 2514, 2515, 2516, 2517, 2518, 2519, 2520, 2521, 2522, 2523, 2524, 2525, 2526, 2527, 2528, 2529, 2530, 2531, 2532, 2533, 2534, 2535, 2536, 2537, 2538, 2539, 2540, 2541, 2542, 2543, 2544, 2545, 2546, 2547, 2548, 2549, 2550, 2551, 2552, 2553, 2554, 2555, 2556, 2557, 2558, 2559, 2560, 2561, 2562, 2563, 2564, 2565, 2566, 2567, 2570, 2571, 2572, 2573, 2574, 2575, 2576, 2577, 2578, 2579, 2580, 2581, 2582, 2583, 2593, 2594, 2595, 2596, 2597, 2598, 2599, 2600, 2601, 2602, 2603, 2604, 2605, 2606, 2607, 2608, 2609, 2610, 2611, 2612, 2613, 2614, 2615, 2616, 2617, 2618, 2619, 2620, 2621, 2622, 2623, 2624, 2625, 2626, 2627, 2628, 2629, 2630, 2631, 2632, 2633, 2634, 2635, 2636, 2637, 2638, 2639, 2640, 2641, 2642, 2643, 2644, 2645, 2646, 2647, 2648, 2649, 3033, 3034, 3035, 3036, 3037, 3038, 3039, 3040, 3041, 3042, 3043, 3044, 3045, 3046, 3047, 3048, 3049, 3050, 3051, 3052, 3053, 3054, 3055, 3056, 3081, 3082, 3083, 3084, 3085, 3086, 3087, 3088, 3089, 3090, 3091, 3092, 3093, 3094, 3095, 3096, 3097, 3098, 3099, 3100, 3101, 3102, 3120, 3121, 3122, 3123, 3124, 3125, 3126, 3127, 3128, 3129, 3130, 3131, 3132, 3133, 3134, 3135, 3136, 3137, 3138, 3139, 3140, 3141, 3142, 3143, 3144, 3145, 3146, 3147, 3148, 3149, 3150, 3151, 3152, 3153, 3154, 3155, 3156, 3157, 3158, 3159, 3160, 3161, 3162, 3163, 3164, 3165, 3166, 3167, 3168, 3169, 3170, 3171, 3172, 3173, 3174, 3175, 3176, 3177, 3178, 3179, 3180, 3181, 3182, 3183, 3184, 3185, 3186, 3187, 3188, 3189, 3190, 3191, 3192, 3193, 3194, 3195, 3196, 3197, 3198, 3199, 3200, 3201, 3202, 3203, 3204, 3205, 3206, 3207, 3208, 3209, 3210, 3211, 3212, 3213, 3214, 3215, 3216, 3217, 3218, 3219, 3220, 3221, 3222, 3223, 3224, 3225, 3226, 3227, 3228, 3229, 3230, 3231, 3232, 3233, 3234, 3235, 3236, 3237, 3238, 3239, 3240, 3241, 3242, 3243, 3244, 3245, 3246, 3247, 3248, 3795, 3796, 3805, 3806, 3807, 3808, 3809, 3810, 3811, 3812, 3813, 3814, 3815, 3816, 3817, 3818, 3819, 3820, 3821, 3822, 3823, 3824, 3876, 3877, 3878, 3879, 3880, 3881, 3882, 3883, 3884, 3885, 3886, 3887, 3888, 3889, 3890, 3891, 3892, 3893, 3894, 3895]

LITTLE_MAZ_LIST = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 700, 702, 703, 704, 705, 706, 707, 708, 709, 710, 712, 714, 942, 943, 944, 947, 948, 949, 950, 951, 952, 953, 3020, 3115]

BREAKME = [2198, 2200, 2202, 4004, 4005, 4008, 4009, 4012, 4013, 4014, 4015, 4016, 4017, 4018, 4019, 4020, 4021, 4022, 4023, 4024, 4025, 4026, 4027, 4028, 4029, 4030, 4031, 4032, 4033, 4034, 4036, 4037, 4038, 4039, 4040, 4041, 4042, 4043, 4044, 4045, 4046, 4047, 4048, 4049, 4050, 4051, 4052, 4053, 4054, 4055, 4057, 4058, 4059, 4074, 4075, 4078, 4079, 4080, 4081, 4082, 4083, 4084, 4085, 4086, 4087, 4088, 4089, 4090, 4091, 4092, 4093, 4120, 4134, 4136, 4137, 4138, 4139, 4140, 4141, 4142, 4143, 4144, 4145, 4146, 4147, 4148, 4149, 4150, 4151, 4152, 4153, 4154, 4155, 4156, 4157, 4158, 4159, 4162, 4163, 4166, 4167, 4170, 4171, 4174, 4175, 4176, 4177, 4178, 4179, 4180, 4181, 4256, 4278, 4279, 4280, 4281, 4282, 4283, 4284, 4285, 4286, 4287, 4288, 4289, 4290, 4291, 4292, 4293, 4294, 4295, 4296, 4297, 4298, 4299, 4300, 4301, 5447, 5489, 5591, 5592, 5595, 5596, 5599, 5600, 5603, 5604, 5607, 5608, 5611, 5612, 5613, 5614, 5617, 5618, 5619, 5621, 5622, 5623, 5624, 5625, 5626, 5627, 5628, 5629, 5630, 5631, 5632, 5633, 5634, 5635, 5636, 5637, 5638, 5639, 5640, 5641, 5642, 5643, 5644, 5645, 5646, 5647, 5648, 5649, 5650, 5651, 5652, 5653, 5655, 5665, 5667, 5669, 5670, 5671, 5672, 5673, 5674, 5675, 5676, 5677, 5678, 5679, 5680, 5681, 5682, 5683, 5684, 5, 654, 1511, 2432, 2668, 3611, 4053, 4061, 4194, 4229, 4251, 4295, 4316, 4364, 4439, 5646, 6506, 8526, 8561, 8592, 8599, 8602, 8605, 9096, 15179, 16177, 16267, 16318, 403, 577, 654, 970, 1061, 2938, 2983, 4049, 4086, 4088, 4099, 5627, 5648, 6505, 7331, 8559, 8560, 8569, 8570, 8599, 8602, 8605, 8615, 8616, 8666, 9074, 15744, 16165, 451, 1117, 1709, 2354, 2775, 3124, 4044, 4082, 4096, 4160, 4623, 5609, 5641, 5642, 8416, 8560, 8565, 8570, 8577, 8589, 8603, 8604, 8605, 16108, 16157, 16201, 16220, 100, 1709, 2602, 2882, 3573, 3997, 4001, 4038, 4045, 4088, 4091, 4184, 4231, 4596, 4686, 5165, 5627, 5629, 6882, 8560, 8562, 8573, 8574, 8600, 8616, 9514, 15921, 16202, 16651]


segments = {
    'test': {'maz': np.array(MAZ_LIST)},  # includes univ
    'little': {'maz': np.array(LITTLE_MAZ_LIST)},
    'univ_east': {'maz': np.arange(MAZ_OFFSET, MAZ_OFFSET + 1080)},
    'full': {},
    'breakme': {'maz': np.array(BREAKME)}
}

parser = argparse.ArgumentParser(description='crop CMAP raw_data')
parser.add_argument('segment_name', metavar='segment_name', type=str, nargs=1,
                    help=f"geography segmentation (e.g. full)")

parser.add_argument('-c', '--check_geography',
                    default=False,
                    action='store_true',
                    help='check consistency of MAZ, TAZ zone_ids and foreign keys & write orphan_households file')

args = parser.parse_args()


segment_name = args.segment_name[0]
check_geography = args.check_geography

assert segment_name in segments.keys(), f"Unknown seg: {segment_name}"

input_dir = os.environ["ASIM_INPUT"]
output_dir = f'{os.environ["BASE_PATH"]}\\activitysim_inputs_{segment_name}'


print(f"segment_name {segment_name}")

print(f"input_dir {input_dir}")
print(f"output_dir {output_dir}")

print(f"check_geography {check_geography}")

if not os.path.isdir(output_dir):
    print(f"creating output directory {output_dir}")
    os.mkdir(output_dir)


def input_path(file_name):
    return os.path.join(input_dir, file_name)


def output_path(file_name):
    return os.path.join(output_dir, file_name)


def patch_maz(df, maz_offset):
    for c in df.columns:
        if c in ['maz', 'OMAZ', 'DMAZ', 'mgra', 'orig_mgra', 'dest_mgra']:
            df[c] += maz_offset
    return df


def read_csv(file_name):
    df = pd.read_csv(input_path(file_name))
    #if MAZ_OFFSET:
    #    df = patch_maz(df, MAZ_OFFSET)
    print(f"read {file_name} {df.shape}")
    return df


def to_csv(df, file_name):
    df.to_csv(output_path(file_name), index=False)
    print(f"write {file_name} {df.shape}")


def crop_omx(omx_file_name, zones, num_outfiles=1):

    skim_data_type = np.float32

    omx_in = omx.open_file(input_path(f"{omx_file_name}.omx"))
    print(f"omx_in shape {omx_in.shape()}")

    offset_map = None
    for mapping_name in omx_in.listMappings():
        _offset_map = np.asanyarray(omx_in.mapentries(mapping_name))
        if offset_map is not None or not (_offset_map == np.arange(1, len(_offset_map) + 1)).all():
            assert offset_map is None or (offset_map == _offset_map).all()
            offset_map = _offset_map

    if offset_map is not None:
        om = pd.Series(offset_map)
        om = om[om.isin(zones.values)]
        nm = om[~om.isin(zones.values)]
        indexes = om.index.values

    else:
        indexes = zones.index.tolist()  # index of TAZ in skim (zero-based, no mapping)
    labels = zones.values  # TAZ zone_ids in omx index order
    
    # create
    if num_outfiles == 1:
        omx_out = [omx.open_file(output_path(f"{omx_file_name}.omx"), 'w')]
    else:
        omx_out = [omx.open_file(output_path(f"{omx_file_name}{i + 1}.omx"), 'w') for i in range(num_outfiles)]

    for omx_file in omx_out:
        omx_file.create_mapping('zone_number', labels)

    iskim = 0
    for mat_name in omx_in.list_matrices():
        # make sure we have a vanilla numpy array, not a CArray
        m = np.asanyarray(omx_in[mat_name]).astype(skim_data_type)
        m = m[indexes, :][:, indexes]
        print(f"{mat_name} {m.shape}")

        omx_file = omx_out[iskim % num_outfiles]
        omx_file[mat_name] = m
        iskim += 1

    omx_in.close()
    
    for omx_file in omx_out:
        omx_file.close()


# non-standard input file names

LAND_USE = "land_use.csv"
HOUSEHOLDS = "households.csv"
PERSONS = "persons.csv"
MAZ_TAZ = "maz.csv"
TAZ = "taz.csv"
SUBZONE = "subzoneData.csv"


if check_geography:

    # ######## check for orphan_households not in any maz in land_use
    land_use = read_csv(LAND_USE)
    land_use = land_use[['maz', 'taz']]
    land_use = land_use.sort_values(['taz', 'maz'])

    households = read_csv(HOUSEHOLDS)
    orphan_households = households[~households.maz.isin(land_use.maz)]
    print(f"{len(orphan_households)} orphan_households")

    # write orphan_households to INPUT directory (since it doesn't belong in output)
    if len(orphan_households) > 0:
        file_name = "orphan_households.csv"
        print(f"writing {file_name} {orphan_households.shape} to {input_path(file_name)}")
        orphan_households.to_csv(input_path(file_name), index=False)

    # ######## check that land_use and maz and taz tables have same MAZs and TAZs

    # could just build maz and taz files, but want to make sure PSRC data is right

    land_use = read_csv(LAND_USE)
    # assert land_use.set_index('MAZ').index.is_monotonic_increasing

    land_use = land_use.sort_values('maz')
    maz = read_csv(MAZ_TAZ).sort_values('MAZ')

    # ### FATAL ###
    if not land_use.maz.isin(maz.MAZ).all():
        print(f"land_use.MAZ not in maz.MAZ\n{land_use.maz[~land_use.maz.isin(maz.maz)]}")
        raise RuntimeError(f"land_use.MAZ not in maz.MAZ")

    if not maz.MAZ.isin(land_use.maz).all():
        print(f"maz.MAZ not in land_use.MAZ\n{maz.maz[~maz.maz.isin(land_use.maz)]}")

    # ### FATAL ###
    if not land_use.taz.isin(maz.TAZ).all():
        print(f"land_use.TAZ not in maz.TAZ\n{land_use.taz[~land_use.taz.isin(maz.taz)]}")
        raise RuntimeError(f"land_use.TAZ not in maz.TAZ")

    if not maz.TAZ.isin(land_use.taz).all():
        print(f"maz.TAZ not in land_use.TAZ\n{maz.taz[~maz.taz.isin(land_use.taz)]}")


# land_use

land_use = read_csv(LAND_USE)

land_use.maz = land_use.maz.astype(int)

ur_land_use = land_use.copy()

slicer = segments[segment_name]
for slice_col, slice_values in slicer.items():
    # print(f"slice {slice_col}: {slice_values}")
    land_use = land_use[land_use[slice_col].isin(slice_values)]

print(f"land_use shape after slicing {land_use.shape}")
to_csv(land_use, 'land_use.csv')


# TAZ

taz = pd.DataFrame({'taz': sorted(ur_land_use.taz.unique())})
taz = taz[taz.taz.isin(land_use["taz"])]
to_csv(taz, TAZ)
# maz_taz


maz_taz = read_csv(MAZ_TAZ).sort_values('MAZ')
maz_taz = maz_taz[maz_taz.MAZ.isin(land_use.maz)]
to_csv(maz_taz, MAZ_TAZ)

# maz to maz

maz_maz_walk = read_csv("maz_to_maz_walk.csv").sort_values(['OMAZ', 'DMAZ'])
maz_maz_bike = read_csv("maz_to_maz_bike.csv").sort_values(['OMAZ', 'DMAZ'])

maz_maz_walk = maz_maz_walk[maz_maz_walk["OMAZ"].isin(land_use["maz"]) & maz_maz_walk["DMAZ"].isin(land_use["maz"])]
maz_maz_bike = maz_maz_bike[maz_maz_bike["OMAZ"].isin(land_use["maz"]) & maz_maz_bike["DMAZ"].isin(land_use["maz"])]

to_csv(maz_maz_walk, "maz_to_maz_walk.csv")
to_csv(maz_maz_bike, "maz_to_maz_bike.csv")

# households

households = read_csv(HOUSEHOLDS)
households = households[households["maz"].isin(land_use["maz"])]
to_csv(households, "households.csv")

# persons

persons = read_csv(PERSONS)
persons = persons[persons["household_id"].isin(households["household_id"])]
to_csv(persons, "persons.csv")

# Subzone (added ASR)

subzone = read_csv(SUBZONE)
subzone = subzone[subzone["subzone"].isin(land_use["maz"])]
to_csv(subzone, "subzone.csv")

# skims

crop_omx('taz_skims', taz.taz, num_outfiles=(4 if segment_name == 'full' else 1))
