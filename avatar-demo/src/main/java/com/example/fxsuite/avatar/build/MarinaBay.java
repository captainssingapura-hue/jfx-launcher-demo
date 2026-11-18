package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Alignment;
import com.example.fxsuite.avatar.sim.Basin;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Pond;
import com.example.fxsuite.avatar.sim.Road;
import com.example.fxsuite.avatar.sim.RoadMap;
import com.example.fxsuite.avatar.sim.Terrain;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Marina Bay as it lies ({@code Demo.MARINA_BAY}), the first stage of laying out the bay round the wall towers: its
 * land, level; its sea, and the bay, its channel and the river's mouth behind the barrage, one water, walled by a steep
 * bank; the larger lakes of its gardens, shallow. In metres from the middle tower, turned so the real towers' long
 * side runs along z, as the wall towers' does. The towers, their hull and its park stand on it as on their own
 * ground ({@link TowerMap}); its roads laid over it ({@link MarinaBayRoads}), their bridges sections of them, left open;
 * its buildings are yet to come.
 *
 * <p>Its lines ({@link #LINES}) are worked out from OpenStreetMap by {@code tools/OsmTerrain.java}, simplified to
 * within 2 m: map data (c) OpenStreetMap contributors, available under the Open Database License (ODbL). They are a
 * database derived from it, and are so licensed. Each line is a kind and its points, x,z in metres: {@code land}, a
 * shore of the land; {@code bound}, where the land is cut off at the world's edge; {@code basin}, a shore of the bay
 * and the river; {@code lake}, a lake's shore, closed.
 */
public final class MarinaBay {

    /** Its lines: the world's edges; the land's shores and bounds; the basin's shores; the lakes'. */
    public static final String LINES = """
            world -2400 2400 -2000 1800
            land -476.2,-2000.0 -642.2,-1429.4 -504.4,-1411.3 -182.7,-1502.2 -180.2,-1510.2 -14.3,-1540.9 -19.7,-1565.2 80.9,-1587.7 85.4,-1566.9 204.1,-1590.9 156.5,-1650.4 156.8,-1671.4 168.2,-1703.5 186.7,-1721.1 320.6,-1823.2 336.4,-1801.7 590.3,-2000.0
            bound 590.3,-2000.0 605.8,-2000.0
            land 605.8,-2000.0 673.0,-1915.2 410.2,-1709.8 687.3,-1354.6 660.3,-1333.4 776.5,-1193.0 829.1,-1124.3 861.4,-1067.9 926.1,-976.4 1358.2,-422.1 1382.0,-381.6 1396.0,-322.7 1393.7,-251.3 1384.6,-219.2 1361.9,-161.4 1335.0,-121.3 1264.5,-43.5 1255.3,-50.0 1250.3,-46.1 1250.7,-21.9 1244.6,-3.4 1437.2,248.2 1593.9,183.7 1696.4,151.9 1740.3,150.7 1793.0,157.1 1840.1,172.0 1886.8,196.4 1905.4,214.7 1907.1,221.8 1905.6,228.5 1893.0,234.1 1883.9,227.6 1885.0,214.2 1837.9,181.3 1791.3,166.6 1741.1,161.3 1712.2,162.9 1976.0,488.3 2077.7,641.9 2083.8,639.0 2091.9,657.6 2117.4,697.9 2145.7,756.3 2136.6,775.0 2152.0,780.4 2221.9,776.4 2243.8,791.3 2252.7,793.9 2254.6,820.3 2277.4,892.6 2307.2,950.9 2376.2,1055.7 2400.0,1079.0
            bound 2400.0,1079.0 2400.0,1800.0 -2400.0,1800.0 -2400.0,-2000.0 -476.2,-2000.0
            basin -2905.6,-71.8 -2913.7,-48.3 -2906.8,42.7 -2912.6,76.8 -2949.4,125.5 -2961.8,134.0 -2974.2,139.3 -2992.0,133.8 -3001.5,127.5 -3081.9,101.9 -3149.0,73.1 -3166.2,73.3 -3178.7,85.7 -3142.2,100.5 -3081.5,130.9 -2995.8,166.0 -2972.0,171.0 -2947.3,165.2 -2923.0,146.0 -2889.6,92.8 -2873.8,60.1 -2874.9,-29.3 -2872.8,-53.0 -2866.1,-59.9 -2855.6,-65.4 -2819.2,-70.4 -2764.4,-65.2 -2668.4,-80.0 -2551.9,-78.4 -2510.7,-63.3 -2481.1,-41.7 -2467.9,-25.9 -2454.6,5.2 -2435.5,69.0 -2428.3,81.4 -2398.8,108.6 -2353.6,141.8 -2351.6,139.0 -2334.3,154.0 -2246.3,168.6 -2204.3,184.5 -2185.4,198.6 -2100.5,242.1 -2081.5,260.9 -2065.2,272.0 -2035.7,282.9 -2015.6,277.8 -1948.3,238.8 -1927.6,231.3 -1914.3,186.8 -1902.4,167.4 -1877.4,133.1 -1853.7,123.7 -1832.2,130.5 -1832.0,134.0 -1785.4,171.6 -1742.7,229.4 -1702.6,249.3 -1625.6,269.7 -1580.7,275.2 -1500.0,272.7 -1473.9,265.2 -1475.0,260.2 -1429.5,247.6 -1428.2,253.0 -1353.6,232.1 -1339.3,221.5 -1334.7,210.1 -1290.9,181.1 -1230.3,133.8 -1232.7,130.7 -1154.2,78.9 -1096.5,34.1 -1077.7,23.4 -1056.7,24.2 -1041.2,28.1 -1025.9,39.3 -998.8,69.7 -949.6,183.0 -953.1,194.8 -934.6,279.8 -921.9,286.0 -924.6,302.7 -917.6,306.0 -919.8,310.5 -916.1,316.6 -919.3,323.2 -909.4,328.0 -912.2,333.7 -908.8,335.4 -910.2,338.1 -874.9,355.3 -862.1,358.5 -812.0,361.2 -770.7,376.4 -692.8,396.1 -685.4,390.5 -687.0,386.1 -662.3,380.2 -640.3,388.6 -630.9,398.6 -634.1,401.9 -629.2,411.0 -520.7,436.5 -501.0,430.2 -468.0,395.0 -421.7,358.4 -379.2,346.5 -327.6,358.6 -180.0,409.3 -205.7,509.8 -203.7,514.1 188.9,607.0 230.2,620.9 271.7,670.2 284.1,726.3 269.9,791.1 48.5,1293.9 5.9,1372.9 -2.1,1431.0 -25.4,1438.3 -24.1,1442.6 -0.6,1436.4 -8.1,1476.0 -40.7,1468.3 -47.7,1483.1 -67.2,1581.0 -62.2,1692.1 -55.5,1725.5 -33.4,1791.0 7.2,1863.3 77.6,1961.4 96.0,1981.4 116.6,2020.0 132.3,2011.8 135.4,2017.5 119.6,2025.6 138.3,2067.1 166.1,2082.6 172.1,2080.6 177.1,2085.1 188.8,2134.0 184.2,2141.5 172.4,2143.6 164.5,2158.1 165.9,2181.2 161.8,2216.6 149.6,2256.1 139.3,2275.8 198.2,2314.7 214.3,2263.3 220.4,2265.2 200.6,2327.3 114.9,2401.6 91.5,2405.6 91.3,2400.6 108.5,2398.2 93.6,2383.8 85.6,2380.3 69.0,2378.1 12.2,2380.0 -72.5,2434.2 -70.6,2449.5 -41.0,2496.4 -40.4,2500.9 -49.3,2502.7 -13.2,2506.6 -7.0,2493.3 0.5,2490.5 13.1,2493.2 32.7,2507.5 54.7,2529.5 88.8,2635.2 104.9,2699.0 192.8,2865.7 193.8,2903.7 204.7,2913.5 142.2,3046.3 136.5,3065.2 164.7,3138.1 158.9,3153.4 152.5,3159.9 209.9,3212.5 240.9,3186.8 248.0,3164.0 243.8,3139.3 230.2,3096.6 231.6,3075.1 278.9,2975.5 296.8,2958.2 321.0,2871.8 319.0,2823.5 282.0,2748.7 266.2,2740.0 238.3,2677.7 226.4,2672.4 227.7,2648.5 206.4,2571.8 200.6,2557.4 196.9,2561.6 192.6,2558.5 220.7,2522.4 303.5,2452.0 445.4,2508.3 580.1,2519.1 586.7,2497.0 582.9,2477.2 617.5,2381.9 659.7,2303.3 724.3,2224.1 817.7,2153.3 854.2,2135.6 918.3,2115.3 883.9,2002.6 833.7,2018.3 821.0,2015.9 816.8,2002.8 824.4,1994.4 837.8,1998.4 840.5,2006.0 883.0,1990.8 884.9,1971.2 804.1,1934.0 739.7,2007.9 636.0,1978.7 661.9,1949.5 703.0,1959.7 723.8,1934.8 611.4,1856.2 539.3,1822.3 510.1,1809.2 284.4,1741.0 275.9,1769.0 256.4,1777.8 237.1,1780.9 215.6,1780.1 182.2,1766.8 167.1,1754.2 152.0,1733.8 143.9,1712.4 143.4,1643.9 149.2,1590.9 159.5,1545.3 192.9,1454.0 214.6,1409.8 322.6,1219.5 465.2,1018.2 552.5,914.2 644.0,813.4 747.2,713.7 850.9,622.0 976.0,522.9 1106.0,433.3 1165.7,396.4 1328.8,303.0 1437.2,248.2 1248.1,1.3 1234.2,12.1 1230.7,8.7 1120.3,97.6 1103.2,74.2 993.7,127.3 989.0,113.4 972.5,119.0 977.9,131.0 800.4,199.6 777.9,196.8 736.9,213.7 667.1,231.7 436.4,283.6 362.7,289.2 175.9,281.5 134.3,290.0 42.7,292.6 -38.7,286.6 -144.1,288.3 -143.8,297.8 -185.7,299.3 -185.9,294.8 -225.9,294.5 -253.4,282.7 -263.9,273.6 -274.9,262.8 -287.9,237.9 -292.0,207.4 -289.6,193.8 -277.8,165.7 -249.9,140.2 -242.5,126.1 -237.5,93.6 -239.5,76.3 -237.2,61.4 -274.0,11.6 -278.6,-0.5 -285.2,-28.6 -285.7,-47.3 -279.7,-86.9 -305.2,-166.0 -339.2,-215.7 -352.7,-251.3 -363.1,-262.7 -374.2,-266.7 -376.0,-272.9 -371.2,-274.3 -379.0,-300.8 -376.1,-301.9 -368.5,-290.8 -359.1,-289.2 -350.5,-296.6 -348.8,-310.2 -353.7,-327.7 -368.1,-354.2 -387.3,-415.7 -386.4,-431.2 -391.7,-450.2 -397.6,-462.2 -403.8,-463.2 -408.1,-458.8 -401.1,-472.0 -405.0,-473.8 -400.4,-486.7 -405.8,-501.6 -420.7,-521.4 -566.7,-478.4 -550.3,-423.3 -573.0,-416.8 -610.2,-349.7 -678.5,-384.9 -701.1,-378.1 -717.9,-433.0 -778.5,-416.7 -775.2,-407.2 -769.6,-404.8 -766.2,-392.5 -761.0,-393.1 -759.0,-399.6 -753.9,-399.4 -764.8,-365.5 -698.4,-349.8 -711.9,-292.5 -736.3,-298.2 -729.1,-328.5 -755.3,-334.6 -763.4,-300.0 -713.0,-288.1 -723.9,-240.1 -760.6,-248.5 -768.5,-212.0 -738.0,-204.6 -736.5,-210.7 -725.6,-208.4 -736.8,-161.3 -785.4,-171.6 -790.4,-160.0 -783.6,-158.0 -786.6,-146.5 -782.2,-144.8 -786.7,-125.5 -791.2,-126.5 -792.1,-122.1 -780.8,-119.1 -783.4,-103.6 -794.6,-106.1 -796.9,-96.8 -792.9,-95.4 -794.5,-88.0 -762.3,-74.3 -752.2,90.5 -750.2,86.8 -746.1,87.3 -739.7,122.7 -750.0,127.0 -762.2,156.6 -753.2,162.3 -734.5,153.5 -729.2,163.1 -750.1,173.3 -803.3,134.9 -875.5,161.6 -876.8,175.4 -871.3,200.1 -876.8,202.7 -892.6,174.7 -913.6,163.7 -910.0,156.3 -911.6,149.4 -904.4,143.5 -939.7,85.3 -954.5,41.1 -951.5,38.8 -966.6,17.4 -959.8,11.3 -1005.0,-38.4 -1006.5,-36.6 -1032.1,-61.5 -1055.5,-72.7 -1054.6,-75.4 -1145.9,-87.0 -1182.8,-86.3 -1253.7,-42.2 -1280.6,-15.4 -1304.8,17.0 -1325.7,68.3 -1333.6,116.9 -1378.0,193.8 -1432.8,213.9 -1528.4,227.9 -1581.5,229.5 -1590.8,229.5 -1595.1,223.9 -1643.4,219.2 -1661.3,212.2 -1681.4,198.4 -1738.5,146.8 -1811.2,91.1 -1824.2,75.8 -1847.5,75.3 -1891.9,85.5 -1915.0,97.9 -1942.5,123.8 -1954.7,136.9 -1973.5,167.7 -1983.4,214.1 -1990.8,231.5 -2020.3,239.8 -2054.1,230.7 -2078.8,201.3 -2142.9,162.1 -2236.6,134.2 -2353.9,91.9 -2379.2,66.3 -2406.4,-9.2 -2432.4,-46.4 -2466.0,-78.4 -2501.7,-100.6 -2541.0,-112.6 -2567.8,-116.4 -2614.6,-114.1 -2705.3,-122.2 -2749.2,-109.5 -2844.0,-103.7 -2866.9,-98.4 -2893.8,-84.2 -2905.6,-71.8
            # a lake
            lake 810.7,77.0 802.4,47.3 804.7,41.6 796.6,17.0 796.8,6.2 805.6,0.0 818.6,12.2 832.5,41.4 831.3,48.1 847.5,65.5 847.3,73.3 839.6,85.2 849.5,90.1 851.5,95.0 846.8,104.2 832.6,109.3 822.3,103.7 810.7,77.0
            # Kingfisher Lake
            lake 672.8,-132.0 752.8,-98.2 753.6,-84.2 748.2,-62.1 727.1,-7.9 727.3,2.7 771.6,46.1 771.8,49.7 753.2,65.7 752.9,70.2 774.0,87.3 776.4,98.2 774.8,113.2 778.4,121.7 791.4,136.1 788.9,150.3 790.9,168.5 800.4,199.6 786.6,196.0 774.8,151.8 758.8,160.8 764.3,175.6 761.9,180.4 722.6,191.9 710.6,170.7 713.2,158.9 745.4,163.2 772.2,145.8 761.2,117.4 738.6,74.8 733.7,69.7 714.1,69.1 712.5,64.2 729.2,63.7 738.7,58.6 749.2,42.0 726.8,15.1 719.6,-5.0 748.3,-79.5 743.8,-94.8 736.3,-100.0 670.9,-128.0 672.8,-132.0
            # a lake
            lake 654.7,1232.3 672.8,1178.8 684.9,1165.9 695.5,1159.4 708.3,1160.4 718.4,1165.7 721.6,1173.3 700.4,1247.4 695.4,1251.8 687.2,1253.2 664.1,1246.4 656.1,1239.6 654.7,1232.3
            # a lake
            lake 1426.6,935.8 1415.8,917.2 1415.5,905.8 1425.4,888.2 1450.2,871.0 1465.2,867.1 1479.1,871.1 1488.6,892.8 1488.6,909.3 1474.2,926.0 1460.2,930.6 1448.9,940.7 1435.9,943.2 1426.6,935.8
            # a lake
            lake 1648.1,718.8 1646.8,701.5 1676.8,625.6 1688.7,607.8 1698.9,606.9 1708.0,611.1 1713.4,623.7 1709.5,638.7 1687.7,672.8 1679.4,712.2 1668.3,728.6 1657.2,728.5 1648.1,718.8
            # a lake
            lake 814.4,1177.8 810.0,1170.8 809.2,1158.0 814.1,1146.4 821.8,1138.6 836.9,1134.3 865.1,1140.0 888.5,1131.1 902.1,1132.0 907.2,1138.4 905.7,1151.8 894.6,1165.3 886.6,1169.1 823.0,1181.3 814.4,1177.8
            # a lake
            lake 1227.8,992.4 1207.5,968.6 1205.3,956.8 1208.8,947.1 1217.7,937.4 1228.6,933.6 1276.7,933.9 1281.8,940.4 1282.2,952.8 1272.7,968.1 1262.5,972.1 1238.9,993.6 1232.3,995.1 1227.8,992.4
            # a lake
            lake 941.7,1285.4 936.8,1281.8 933.5,1271.1 935.7,1263.4 954.7,1248.3 968.1,1231.9 981.2,1227.3 1013.5,1237.6 1089.7,1224.1 1099.1,1217.9 1143.8,1151.6 1151.9,1148.1 1160.1,1151.6 1165.3,1162.6 1161.0,1174.4 1149.9,1188.5 1131.4,1197.5 1103.8,1246.9 1095.0,1251.2 948.9,1286.4 941.7,1285.4
            # Dragonfly Lake
            lake 232.7,-51.3 233.5,-16.7 249.2,18.0 272.1,42.8 310.7,70.7 308.2,74.4 332.9,85.0 332.1,88.7 325.4,88.4 326.0,98.7 312.6,102.5 315.1,107.2 285.0,113.4 238.7,130.2 216.5,145.1 208.2,157.6 175.9,281.5 162.7,282.3 193.2,159.2 185.8,157.9 200.8,52.7 208.0,-74.0 194.5,-219.9 183.3,-290.4 126.8,-514.3 82.2,-662.6 90.6,-665.1 100.7,-657.9 108.5,-677.5 121.4,-681.3 123.7,-673.2 115.1,-670.6 106.9,-651.3 111.2,-631.2 104.9,-599.2 128.1,-569.4 130.3,-557.2 125.9,-528.6 127.8,-523.6 132.6,-519.4 132.4,-514.9 165.9,-483.5 168.7,-476.0 157.4,-436.5 159.2,-421.0 169.5,-397.6 184.3,-380.0 199.0,-372.5 207.9,-374.7 211.0,-379.2 218.7,-404.8 232.3,-424.2 236.4,-420.2 224.9,-402.4 221.0,-385.1 224.2,-373.7 232.6,-363.5 245.1,-357.0 242.2,-352.0 228.5,-359.7 208.1,-365.8 201.6,-366.2 196.3,-359.2 193.7,-323.4 196.8,-296.3 204.6,-270.0 213.5,-247.6 226.7,-228.7 234.8,-227.1 243.2,-235.5 255.0,-262.7 266.8,-261.0 251.2,-216.5 246.5,-178.9 253.6,-155.2 276.0,-129.0 274.7,-123.7 293.0,-110.4 314.3,-101.9 305.5,-90.3 268.0,-111.8 251.2,-116.9 235.3,-70.2 232.7,-51.3
            # origin 1.2836812,103.8607101; turned 16.21 degrees
            # Marina Bay Sands Tower 1 at -20.4,-118.5, its long side at 58.12 degrees from z
            # Marina Bay Sands Tower 3 at 5.0,106.6, its long side at 9.11 degrees from z
            # Marina Bay Sands Tower 2 at -0.0,-0.0, its long side at 180.00 degrees from z
            # water 46342764 Geylang River: 153717 m2 at 2093,3343
            # water 46522851 -: 682 m2 at -2310,1131
            # water 121874612 Fountain of Wealth: 1758 m2 at -534,1121
            # water 166311286 -: 471 m2 at -924,804
            # water 166553472 -: 1233 m2 at 1146,-8
            # water 171981650 -: 1185 m2 at 365,-687
            # water 171981653 -: 1647 m2 at 246,-713
            # water 171981655 -: 1456 m2 at 559,-223
            # water 171981657 Water Lily Pond: 2216 m2 at 660,69
            # water 171981659 -: 3101 m2 at 826,63
            # water 171981661 Kingfisher Lake: 5824 m2 at 745,50
            # water 171981664 -: 1439 m2 at 454,-425
            # water 172288974 -: 249 m2 at 586,81
            # water 172299378 -: 57 m2 at 624,101
            # water 172299381 -: 50 m2 at 564,103
            # water 172299382 -: 380 m2 at 327,-223
            # water 172307481 Rain Oculus: 388 m2 at -223,-37
            # water 172469228 -: 373 m2 at 1063,52
            # water 174718140 -: 728 m2 at 682,860
            # water 393597600 -: 4057 m2 at 688,1208
            # water 393597601 -: 3856 m2 at 1450,910
            # water 393597602 -: 3754 m2 at 1679,671
            # water 393597604 -: 3485 m2 at 853,1155
            # water 393597605 -: 3251 m2 at 1242,962
            # water 393597607 -: 7832 m2 at 1052,1223
            # water 455092449 -: 444 m2 at -2192,-639
            # water 455099074 -: 23 m2 at -2761,706
            # water 620580643 -: 152 m2 at -184,250
            # water 687668442 -: 1068 m2 at -235,252
            # water 687994237 -: 885 m2 at -658,1553
            # water 688830991 -: 66 m2 at -1071,199
            # water 688879275 -: 97 m2 at -827,529
            # water 688879280 -: 58 m2 at -1187,865
            # water 690204721 -: 77 m2 at 196,-549
            # water 743763993 -: 924 m2 at -1491,1076
            # water 827855855 -: 2525 m2 at -240,196
            # water 827855909 -: 306 m2 at -201,242
            # water 913028002 -: 981 m2 at 87,-401
            # water 934999013 -: 839 m2 at 1365,1495
            # water 934999014 -: 1074 m2 at 1468,1693
            # water 934999016 -: 1035 m2 at 1439,1527
            # water 934999017 -: 1079 m2 at 1495,1624
            # water 1015224188 -: 56 m2 at -1329,-640
            # water 1168513595 -: 80 m2 at -1306,-170
            # water 1228813828 -: 34 m2 at -1026,966
            # water 1239178503 -: 398 m2 at 762,1206
            # water 1347285037 -: 198 m2 at -2227,1135
            """;

    /** How high the water of the sea, the bay and the river is; how high the lakes'. */
    public static final double SEA = -300;
    public static final double LAKES = -50;
    /** How deep the sea, the bay and the river are, their bank how wide; the lakes', shallow and gently sloped. */
    static final double DEEP = 800;
    static final double BANK = 1_200;
    static final double LAKE_DEEP = 200;
    static final double LAKE_BANK = 600;
    /** Where the avatar starts: across the bay from the towers, by the river's mouth, facing them. */
    public static final Vec3 START = new Vec3(-80_000, 0, 5_000);

    private static Land land;
    private static List<Road> roads;
    private static List<List<Vec2>> lakes;
    private static double[] world;

    private MarinaBay() {
    }

    /** Its lines of one kind, in centimetres. */
    static List<List<Vec2>> lines(final String kind) {
        final List<List<Vec2>> lines = new ArrayList<>();
        for (final String raw : LINES.split("\\R")) {
            final String line = raw.replaceFirst("#.*", "").strip();
            if (!line.startsWith(kind + " ")) {
                continue;
            }
            final List<Vec2> points = new ArrayList<>();
            for (final String p : line.substring(kind.length() + 1).strip().split("\\s+")) {
                final String[] xz = p.split(",");
                points.add(new Vec2(Double.parseDouble(xz[0]) * 100, Double.parseDouble(xz[1]) * 100));
            }
            lines.add(points);
        }
        return lines;
    }

    /** Its world's edges, in centimetres: west, east, south, north. */
    static synchronized double[] world() {
        if (world == null) {
            final String[] w = LINES.lines().map(String::strip).filter(l -> l.startsWith("world ")).findFirst()
                    .orElseThrow().split("\\s+");
            world = new double[] {Double.parseDouble(w[1]) * 100, Double.parseDouble(w[2]) * 100,
                    Double.parseDouble(w[3]) * 100, Double.parseDouble(w[4]) * 100};
        }
        return world;
    }

    /**
     * Its land: level at 0, the towers' ground; the sea round it and the bay within it one basin, the land's shores
     * and the bay's, the sea where a point is outside the land or within the bay; the lakes another, shallower.
     */
    public static synchronized Land land() {
        if (land == null) {
            final double[] w = world();
            final List<List<Vec2>> shores = new ArrayList<>(lines("land"));
            shores.addAll(lines("basin"));
            final Basin sea = new Basin(shores, lines("bound"), true, DEEP, BANK, w[0], w[1], w[2], w[3]);
            // the lakes' basin only as far as they are, and their banks
            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            for (final List<Vec2> lake : lakes()) {
                for (final Vec2 p : lake) {
                    minX = Math.min(minX, p.x());
                    maxX = Math.max(maxX, p.x());
                    minZ = Math.min(minZ, p.z());
                    maxZ = Math.max(maxZ, p.z());
                }
            }
            final Basin lake = new Basin(lakes(), List.of(), false, LAKE_DEEP, LAKE_BANK, minX - LAKE_BANK,
                    maxX + LAKE_BANK, minZ - LAKE_BANK, maxZ + LAKE_BANK);
            final Terrain flat = new Terrain("marina bay", w[0], w[2], w[1], w[3], 1_000, 0, 11, Terrain.Noise.NONE,
                    List.of(), List.of());
            final Land ground = new Land(flat.survey(), List.of(sea, lake));
            // its roads laid over it, in turn, each meeting those before at their junctions; their bridges carried
            // over, left open
            roads = RoadMap.parse(MarinaBayRoads.text()).laid(ground, Alignment.STEP);
            land = ground.with(roads);
        }
        return land;
    }

    /** Its roads, as laid on its land ({@link MarinaBayRoads}). */
    public static synchronized List<Road> roads() {
        land();
        return roads;
    }

    /** Its lakes' shores, closed. */
    static synchronized List<List<Vec2>> lakes() {
        if (lakes == null) {
            lakes = lines("lake");
        }
        return lakes;
    }

    /**
     * Where water lies still: the sea, the bay and the river, one sheet over all of it at {@link #SEA}, showing
     * wherever the land is lower; each lake its own, at {@link #LAKES}; and the pool on the towers' hull.
     */
    public static List<Pond> ponds() {
        final double[] w = world();
        final List<Pond> ponds = new ArrayList<>();
        ponds.add(new Pond(List.of(new Vec2(w[0], w[2]), new Vec2(w[1], w[2]), new Vec2(w[1], w[3]),
                new Vec2(w[0], w[3])), SEA));
        for (final List<Vec2> lake : lakes()) {
            ponds.add(new Pond(lake.subList(0, lake.size() - 1), LAKES));
        }
        ponds.addAll(TowerMap.ponds());
        return ponds;
    }

    /** Tells the simulation all of it: the towers and what is on them, its water, where the avatar starts, its land. */
    public static void tell() {
        Course.bay(TowerMap::buildings, MarinaBay::ponds, () -> START, MarinaBay::land, TowerMap::people);
    }
}
