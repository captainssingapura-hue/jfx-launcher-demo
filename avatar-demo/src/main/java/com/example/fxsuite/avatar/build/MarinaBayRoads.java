package com.example.fxsuite.avatar.build;

/**
 * Marina Bay's roads, as the road map's text ({@code RoadMap.parse}), in metres from the middle tower, turned as
 * {@link MarinaBay} is: their kinds, by their class and their lanes; their junctions, every point two roads share; each
 * road its stretches joined end to end where it carries on under the same name the same way, its bridges sections of
 * it (left open, for now: nothing is built on them), its tunnels left out.
 *
 * <p>Worked out from OpenStreetMap by {@code tools/OsmTerrain.java}: map data (c) OpenStreetMap contributors,
 * available under the Open Database License (ODbL). A database derived from it, and so licensed. In parts, as a
 * class's constants are each so long at most.
 */
final class MarinaBayRoads {

    private MarinaBayRoads() {
    }

    /** All of it, its parts in order. */
    static String text() {
        return String.join("", PART_1, PART_2, PART_3);
    }

    private static final String PART_1 = """
            kind primary-2 width=8.00m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=2x3.50m inner=0.5m
            kind residential-2 width=8.00m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=2x3.50m inner=0.5m
            kind tertiary-2 width=8.00m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=2x3.50m inner=0.5m
            kind residential-1 width=4.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=1x3.50m inner=0.5m
            kind motorway-2 width=9.30m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=2x3.65m inner=0.5m
            kind primary-4 width=15.00m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=4x3.50m inner=0.5m
            kind trunk-4 width=16.60m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=4x3.65m inner=0.5m
            kind secondary-4 width=15.00m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=4x3.50m inner=0.5m
            kind secondary-2 width=8.00m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=2x3.50m inner=0.5m
            kind primary-3 width=11.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=3x3.50m inner=0.5m
            kind residential-3 width=11.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=3x3.50m inner=0.5m
            kind primary-5 width=18.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=5x3.50m inner=0.5m
            kind tertiary-1 width=4.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=1x3.50m inner=0.5m
            kind primary-link-3 width=11.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=3x3.50m inner=0.5m
            kind secondary-5 width=18.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=5x3.50m inner=0.5m
            kind trunk-6 width=23.90m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=6x3.65m inner=0.5m
            kind trunk-link-2 width=9.30m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=2x3.65m inner=0.5m
            kind primary-6 width=22.00m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=6x3.50m inner=0.5m
            kind trunk-3 width=12.95m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=3x3.65m inner=0.5m
            kind motorway-link-2 width=9.30m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=2x3.65m inner=0.5m
            kind trunk-5 width=20.25m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=5x3.65m inner=0.5m
            kind tertiary-4 width=15.00m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=4x3.50m inner=0.5m
            kind trunk-2 width=9.30m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=2x3.65m inner=0.5m
            kind tertiary-3 width=11.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=3x3.50m inner=0.5m
            kind secondary-3 width=11.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=3x3.50m inner=0.5m
            kind motorway-link-1 width=5.65m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=1x3.65m inner=0.5m
            kind unclassified-1 width=4.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=1x3.50m inner=0.5m
            kind unclassified-2 width=8.00m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=2x3.50m inner=0.5m
            kind primary-link-2 width=8.00m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=2x3.50m inner=0.5m
            kind primary-link-1 width=4.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=1x3.50m inner=0.5m
            kind motorway-3 width=12.95m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=3x3.65m inner=0.5m
            kind tertiary-5 width=18.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=5x3.50m inner=0.5m
            kind motorway-4 width=16.60m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=4x3.65m inner=0.5m
            kind trunk-link-4 width=16.60m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=4x3.65m inner=0.5m
            kind motorway-5 width=20.25m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=5x3.65m inner=0.5m
            kind trunk-link-3 width=12.95m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=3x3.65m inner=0.5m
            kind motorway-6 width=23.90m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=6x3.65m inner=0.5m
            kind secondary-link-1 width=4.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=1x3.50m inner=0.5m
            kind trunk-link-1 width=5.65m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=1x3.65m inner=0.5m
            kind motorway-link-3 width=12.95m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=3x3.65m inner=0.5m
            kind unclassified-3 width=11.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=3x3.50m inner=0.5m
            kind secondary-1 width=4.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=1x3.50m inner=0.5m
            kind primary-7 width=25.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=7x3.50m inner=0.5m
            kind tertiary-link-1 width=4.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=1x3.50m inner=0.5m
            kind primary-1 width=4.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=1x3.50m inner=0.5m
            kind residential-4 width=15.00m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=4x3.50m inner=0.5m
            kind motorway-link-4 width=16.60m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=4x3.65m inner=0.5m
            kind residential-5 width=18.50m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=5x3.50m inner=0.5m
            kind unclassified-4 width=15.00m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=6m vertical=30m lanes=4x3.50m inner=0.5m
            kind trunk-1 width=5.65m surface=asphalt grade=8% curve=1/8m corners slope=1:2 camber=2% transition=12m vertical=30m lanes=1x3.65m inner=0.5m
            node n25451929 at=908.1,1594.1
            node n25455287 at=1057.5,1662.0
            node n26781973 at=-1574.0,-512.8
            node n26782016 at=232.6,1289.3
            node n26782044 at=-403.2,1273.8
            node n26782131 at=-1627.7,-736.4
            node n26782154 at=-1883.8,-1105.9
            node n26782168 at=-2388.3,-1436.6
            node n26782185 at=-1345.8,-614.9
            node n26782230 at=-1133.4,-560.9
            node n26782248 at=-1832.6,-1004.0
            node n26782293 at=-1284.9,-630.8
            node n26782339 at=-1785.7,-958.7
            node n26782342 at=-1254.9,-533.1
            node n74392496 at=-2306.8,526.1
            node n74405854 at=-2154.6,289.2
            node n74405856 at=-2370.3,340.9
            node n74407251 at=-2187.3,481.1
            node n74442029 at=-2029.7,431.7
            node n74442036 at=-1532.0,305.7
            node n74444085 at=-2036.0,88.6
            node n74444087 at=-2018.9,141.7
            node n74444100 at=-2018.9,83.5
            node n74454947 at=-2180.6,168.6
            node n74454952 at=-2112.4,459.3
            node n74454975 at=-2292.1,1080.4
            node n74460234 at=-2187.7,112.8
            node n74460245 at=-1689.4,-53.5
            node n96443466 at=-1996.9,-1235.8
            node n172510077 at=-815.3,855.8
            node n206833639 at=-1307.1,1031.2
            node n206833694 at=-1211.0,1246.0
            node n206833715 at=-1164.4,1355.5
            node n232082341 at=-963.7,900.5
            node n232082494 at=-919.3,1085.1
            node n232082528 at=-872.6,1224.6
            node n232082907 at=-642.4,1672.0
            node n232083078 at=-629.7,1663.7
            node n232083643 at=-740.4,1466.4
            node n232119579 at=-991.3,1440.8
            node n232119616 at=-1041.1,1304.1
            node n232119697 at=-1099.4,1154.0
            node n232119730 at=-1166.2,978.8
            node n232147312 at=-1232.1,782.6
            node n232147335 at=-1290.7,555.6
            node n232147366 at=-1344.1,359.4
            node n232207249 at=-1454.9,590.1
            node n232207435 at=-1480.5,412.3
            node n232207661 at=-1502.1,400.6
            node n232207771 at=-1468.1,593.0
            node n232207810 at=-1421.8,834.7
            node n232207994 at=-1537.0,293.9
            node n232278983 at=-1254.8,322.3
            node n232353088 at=-1552.9,689.2
            node n232353226 at=-1033.3,727.0
            node n232353329 at=-1517.1,857.9
            node n232353783 at=-1200.6,528.2
            node n232353925 at=-1152.0,320.6
            node n232376467 at=-1065.9,692.9
            node n232376514 at=-1100.3,746.5
            node n233421104 at=-1408.9,831.3
            node n233432379 at=-1322.6,1037.3
            node n233432515 at=-1180.3,1362.5
            node n233432542 at=-1174.1,1378.4
            node n233497138 at=-1742.3,1209.4
            node n233497179 at=-796.1,851.2
            node n233497244 at=-1636.6,1167.4
            node n233544132 at=-1883.4,1180.8
            node n233602622 at=-1082.8,1195.4
            node n233602664 at=-823.2,1345.7
            node n233782664 at=-515.6,1343.9
            node n233783186 at=-712.1,1147.5
            node n233783297 at=-731.6,1153.6
            node n233801197 at=-1283.5,1407.1
            node n233801222 at=-1392.1,1451.4
            node n233801368 at=-862.3,1206.5
            node n233879421 at=-845.3,634.3
            node n233879440 at=-740.4,587.1
            node n233879603 at=-152.0,632.7
            node n233879721 at=19.3,857.6
            node n233879753 at=-864.4,643.3
            node n233879798 at=-685.1,824.7
            node n233879872 at=-223.1,815.9
            node n233879899 at=-212.3,819.3
            node n233880373 at=-639.5,813.0
            node n234839820 at=-58.0,958.3
            node n235069384 at=-187.7,1494.9
            node n235071053 at=-201.1,1479.2
            node n235129976 at=-1679.4,1078.6
            node n235130001 at=-1748.3,1105.2
            node n235180256 at=-1429.0,1081.5
            node n235180430 at=-2023.1,1063.2
            node n236429505 at=-1870.7,1141.2
            node n237012067 at=-1409.8,118.4
            node n237012083 at=-1434.7,37.5
            node n237012110 at=-1466.1,-100.7
            node n237012130 at=-1501.9,-232.5
            node n237012179 at=-1559.5,-459.1
            node n237012287 at=-832.7,94.6
            node n237054576 at=-812.0,67.1
            node n237054768 at=-831.7,70.6
            node n237054972 at=-886.9,-429.8
            node n237055087 at=-1101.5,-1195.4
            node n237055115 at=-1211.5,-1155.1
            node n237055163 at=-1121.6,-886.4
            node n237089541 at=-1211.9,-860.9
            node n237089604 at=-1263.9,-1040.1
            node n237089753 at=-1680.5,-865.8
            node n237089858 at=-1510.4,-967.1
            node n237105259 at=-1491.7,-1219.3
            node n237105451 at=-1475.7,-1369.9
            node n237105802 at=-1651.8,-1036.4
            node n237153674 at=-1592.7,-1514.5
            node n237153849 at=-1554.8,-1711.3
            node n237154096 at=-1511.3,-1533.8
            node n237154127 at=-1985.1,-770.7
            node n237154196 at=-1145.4,-1332.8
            node n237154299 at=-1242.3,-1543.3
            node n237154323 at=-1278.3,-1625.5
            node n237154345 at=-1457.8,-1680.4
            node n237154414 at=-1368.3,-1487.6
            node n237154438 at=-1276.7,-1277.0
            node n237154569 at=-1760.4,-1621.0
            node n237170319 at=-1517.1,296.6
            node n237170343 at=-1563.6,182.8
            node n237170369 at=-1598.4,97.4
            node n237170380 at=-1669.7,-60.1
            node n237170391 at=-1674.8,-71.3
            node n237170487 at=-1724.5,-182.5
            node n237170508 at=-1746.3,-234.3
            node n237170538 at=-1818.7,-389.4
            node n237170655 at=-1940.2,-665.6
            node n237170670 at=-2062.2,-951.6
            node n237170711 at=-2119.8,-1075.6
            node n237218543 at=-1636.0,15.1
            node n237218629 at=-1448.1,-19.6
            node n237218667 at=-1468.8,-111.8
            node n237218696 at=-1935.3,-602.4
            node n237218756 at=-1694.3,-64.9
            node n237218779 at=-2132.9,-1058.7
            node n237218837 at=-1782.4,-259.3
            node n237218865 at=-2002.7,-755.3
            node n237219117 at=-1839.9,-382.7
            node n237219180 at=-1744.9,-176.9
            node n237219274 at=-1392.1,-113.1
            node n237251040 at=-1257.2,-155.9
            node n237251482 at=-1778.3,-39.8
            node n237251504 at=-2100.3,73.5
            node n237278876 at=-1633.5,-1736.6
            node n237278991 at=-1667.7,-1747.3
            node n237279064 at=-2097.0,-1132.9
            node n237279254 at=-1873.8,-1747.3
            node n237296520 at=-1966.4,-329.4
            node n237296580 at=-2057.8,-250.1
            node n237296663 at=-2061.8,-202.9
            node n237296721 at=-2169.6,-141.9
            node n237296820 at=-2235.9,-115.0
            node n237296882 at=-2229.5,-80.7
            node n237296972 at=-1914.6,-133.1
            node n237315539 at=-924.9,-390.8
            node n237348741 at=-2140.2,-1075.2
            node n237348779 at=-2126.8,-1093.3
            node n240002104 at=-969.6,-711.7
            node n240686973 at=-1803.4,1209.8
            node n241595237 at=-1065.2,-685.6
            node n241595238 at=-1384.4,-1111.5
            node n241595239 at=-1363.0,-1074.0
            node n243208171 at=-1988.6,-122.6
            node n243210137 at=-2226.0,-1358.7
            node n243213817 at=-2284.2,-90.7
            node n243233999 at=-888.0,-72.6
            node n243234023 at=-2128.3,880.8
            node n243413548 at=-1741.9,1064.0
            node n243494149 at=819.5,1586.4
            node n243668557 at=-2043.4,-1058.8
            node n243668558 at=-2019.5,-1056.9
            node n243668561 at=-1925.6,-1028.8
            node n243668563 at=-1941.8,-915.2
            node n243668564 at=-1944.4,-814.3
            node n243668569 at=-2010.7,-1089.7
            node n246754375 at=-1370.4,-3.7
            node n246759045 at=-1034.0,-585.6
            node n246800406 at=-1028.5,-418.4
            node n246800416 at=-1843.4,-449.1
            node n246800441 at=-1894.9,-562.8
            node n246800493 at=-1879.2,-968.6
            node n246800552 at=-1867.9,368.6
            node n246800568 at=-1925.0,252.0
            node n246800769 at=-1739.6,-757.2
            node n246800771 at=-1632.4,-751.4
            node n246800772 at=-1750.5,-799.9
            node n246800789 at=-1765.2,-850.1
            node n246800809 at=-1709.5,-1210.4
            node n246800861 at=-1729.6,-1209.2
            node n246800870 at=-1148.3,-1181.6
            node n246801932 at=-2049.2,-1770.1
            node n246801933 at=-1888.9,-1784.6
            node n246801934 at=-1880.4,-1815.6
            node n246801940 at=-2328.0,-1755.3
            node n246801944 at=-2305.4,-1495.7
            node n246801947 at=-2180.2,-1439.9
            node n246801952 at=-1218.4,-646.5
            node n246801953 at=-1159.3,-659.9
            node n246801955 at=-1272.9,-846.0
            node n246806211 at=-1296.7,-1663.4
            node n246806248 at=-1205.0,-1463.5
            node n246806260 at=-1464.1,-1711.5
            node n246806266 at=-1725.3,-1272.3
            node n246806272 at=-1770.5,-1610.6
            node n246806289 at=-1523.1,-837.7
            node n246806296 at=-1558.5,-704.2
            node n246806298 at=-1516.2,-285.5
            node n246806302 at=-1353.8,-267.1
            node n246806303 at=-1406.3,-497.6
            node n246806310 at=-1249.9,-138.7
            node n246810545 at=-1068.6,-424.5
            node n246810548 at=-1202.5,-316.2
            node n246810549 at=-1181.7,-329.9
            node n246810568 at=-1140.8,-171.5
            node n246810582 at=-1185.4,-171.8
            node n246810583 at=-1008.4,-292.0
            node n246810673 at=-1101.7,-461.9
            node n246817239 at=-998.9,-499.8
            node n247008452 at=268.0,1713.6
            node n247008459 at=428.8,1466.7
            node n248420531 at=-668.9,-708.9
            node n248527325 at=-791.4,-458.0
            node n248527331 at=-941.3,-611.3
            node n248536068 at=-742.3,-627.3
            node n249690363 at=-1616.8,-696.8
            node n249690365 at=-1980.1,-580.2
            node n249692307 at=-2245.1,-156.8
            node n249692339 at=-2233.6,-177.5
            node n249693001 at=-2167.8,-94.2
            node n249766690 at=-1036.5,-155.1
            node n249766692 at=-934.7,-71.9
            node n249766698 at=-1045.5,-581.6
            node n249792083 at=-2309.3,-955.2
            node n249798852 at=-2161.8,-733.3
            node n323137286 at=-1399.5,187.1
            node n388569722 at=-2261.2,-1971.1
            node n388569724 at=-1869.9,-1844.2
            node n388569725 at=-1674.4,-1780.4
            node n408733173 at=-490.3,1120.3
            node n408999670 at=-2067.8,1173.3
            node n408999675 at=-2210.5,1177.5
            node n408999681 at=-2191.7,1066.3
            node n409000687 at=-2374.3,1104.2
            node n410055846 at=-1406.9,1133.7
            node n440009224 at=-896.0,1153.5
            node n440009225 at=-1072.0,1223.3
            node n471501777 at=-2232.7,305.5
            node n472730475 at=-1561.4,-1223.9
            node n479983003 at=-163.9,629.9
            node n554037746 at=-1276.4,-1530.4
            node n554037752 at=-1245.1,-1438.1
            node n554037804 at=-1550.3,-1313.5
            node n555760573 at=-1020.2,-698.3
            node n564553552 at=-1773.4,-41.7
            node n571492380 at=-2270.4,-70.8
            node n571492385 at=-2219.0,-34.5
            node n571492447 at=-1998.8,419.8
            node n571883509 at=-2381.4,1184.1
            node n595254198 at=-1523.1,1404.9
            node n595254399 at=-946.4,893.7
            node n595267991 at=-1261.6,1459.3
            node n595267994 at=-1156.9,1416.0
            node n595268007 at=-1111.1,1515.7
            node n595497898 at=-411.1,1087.3
            node n595497900 at=-562.9,978.7
            node n595497945 at=-578.1,798.7
            node n597413159 at=-1832.0,-153.3
            node n602549545 at=-1602.1,-634.4
            node n612392123 at=-2302.5,536.7
            node n612392126 at=-2109.8,471.0
            node n612392130 at=-1884.7,392.0
            node n612392154 at=-2099.4,523.5
            node n621083492 at=-1167.2,-873.2
            node n682933110 at=-2393.0,275.2
            node n682933923 at=-2376.0,323.3
            node n682938046 at=-2182.3,490.0
            node n1130779933 at=-501.5,1091.3
            node n1130779944 at=-562.5,1086.6
            node n1145428485 at=-285.5,1126.3
            node n1145435573 at=-18.7,427.5
            node n1145466122 at=-75.4,-668.9
            node n1145506404 at=-883.8,-1459.8
            node n1149139906 at=-854.0,-154.4
            node n1149139921 at=-857.9,-209.6
            node n1271162085 at=-539.1,1163.6
            node n1275666709 at=-2201.9,872.8
            node n1275666710 at=-2252.5,1011.5
            node n1275666711 at=-2229.1,1068.9
            node n1275666714 at=-2098.7,455.5
            node n1275666718 at=-2096.1,468.1
            node n1275666727 at=-2178.1,111.1
            node n1316829262 at=-609.7,-511.6
            node n1318544653 at=-1253.1,-1227.1
            node n1318544659 at=-1259.1,-1129.0
            node n1318544672 at=-1250.6,-1201.1
            node n1318544675 at=-1271.1,-1069.1
            node n1318569081 at=-1317.6,-1096.6
            node n1318603811 at=-1803.3,-1587.8
            node n1318603823 at=-1801.0,-1656.9
            node n1318603856 at=-1498.7,-794.6
            node n1318603861 at=-1489.1,-767.1
            node n1347466379 at=-1927.2,1313.6
            node n1363236253 at=-504.0,1152.0
            node n1363236276 at=-521.4,1078.5
            node n1363263232 at=-2257.0,830.4
            node n1363263376 at=-2151.1,717.4
            node n1363263413 at=-2177.2,701.2
            node n1363263434 at=-2064.7,957.1
            node n1363263485 at=-2128.9,861.4
            node n1363263517 at=-2148.4,748.0
            node n1363263536 at=-2166.1,716.0
            node n1363263538 at=-2185.2,795.8
            node n1363263545 at=-2303.7,874.0
            node n1363277802 at=-2352.4,935.5
            node n1363293957 at=-2061.4,-172.7
            node n1363294008 at=-2098.4,-234.8
            node n1363294010 at=-2069.4,-154.0
            node n1363487255 at=-848.1,-1459.5
            node n1398048318 at=-1400.6,85.1
            node n1398048399 at=-1414.0,105.4
            node n1398080906 at=-843.6,1191.6
            node n1408671492 at=-1497.2,920.3
            node n1538307112 at=-238.3,1461.0
            node n1538307199 at=-357.4,1294.6
            node n1538307248 at=-233.9,1451.4
            node n1538307290 at=-206.9,1505.4
            node n1538307293 at=-203.7,1490.0
            node n1539835288 at=-607.8,-1300.1
            node n1539835307 at=-398.5,-1375.5
            node n1539835341 at=-492.6,-1371.1
            node n1539883480 at=324.9,-1005.7
            node n1539883491 at=-239.0,-620.9
            node n1539883493 at=672.6,-1106.4
            node n1539883507 at=1170.5,-178.5
            node n1539883528 at=-428.4,-565.1
            node n1539883536 at=1162.4,-194.5
            node n1539883549 at=310.4,-1001.3
            node n1539883582 at=-224.1,-625.3
            node n1539888324 at=-785.1,-764.4
            node n1539897210 at=-135.5,-865.8
            node n1539897211 at=-931.0,-1248.5
            node n1539897215 at=-840.7,-960.3
            node n1539897224 at=-282.6,-821.1
            node n1539897226 at=-297.7,-816.5
            node n1539944025 at=-750.4,-986.5
            node n1539944050 at=-512.5,-845.8
            node n1539944070 at=-565.1,-1039.6
            node n1539944086 at=-692.7,-791.4
            node n1539944100 at=-53.0,-675.5
            node n1539944119 at=-110.6,-873.4
            node n1629037202 at=-1668.3,-870.6
            node n1629037257 at=-1938.2,-853.0
            node n1629037271 at=-1926.5,-809.2
            node n1629037351 at=-1694.2,-189.4
            node n1629037383 at=-1710.6,-151.7
            node n1629337142 at=386.4,-806.1
            node n1629337146 at=372.3,-801.9
            node n1629524122 at=-486.5,-762.4
            node n1633135165 at=-213.9,-1139.4
            node n1633135168 at=-367.5,-1106.9
            node n1633162341 at=-969.7,-410.8
            node n1637003311 at=656.3,-380.2
            node n1675287301 at=-2052.8,1262.8
            node n1683121556 at=-800.2,910.7
            node n1683121661 at=-877.1,871.8
            node n1684391416 at=-2358.8,-1373.8
            node n1777643013 at=287.4,-1632.1
            node n1783639818 at=-194.2,1445.3
            node n1790098230 at=-2259.2,-183.4
            node n1790098242 at=-2224.4,-120.8
            node n1790124104 at=-2218.3,-82.8
            node n1790124129 at=-2153.3,105.7
            node n1790124158 at=-2171.0,143.7
            node n1790158061 at=-2063.6,90.0
            node n1790324627 at=-1834.2,1.1
            node n1790366566 at=-2179.5,734.2
            node n1814187447 at=-1585.4,-1026.8
            node n1816580034 at=-1623.3,89.5
            node n1816580050 at=-1563.5,290.7
            node n1818199237 at=-1703.7,1089.1
            node n1829374038 at=603.5,-63.2
            node n1829374136 at=654.8,-106.0
            node n1829374168 at=622.8,-100.9
            node n1829374182 at=629.2,-15.5
            node n1829436720 at=-2160.2,-1472.5
            node n1829622670 at=-2330.5,-1782.4
            node n1829822434 at=-1928.8,-1831.1
            node n1829822435 at=-2310.9,-1401.3
            node n1829822444 at=-2025.2,-1251.6
            node n1829822445 at=-2159.6,-1113.1
            node n1829822448 at=-1745.2,-1639.1
            node n1829822450 at=-2169.0,-1007.3
            node n1829822453 at=-2142.8,-1130.6
            node n1829822456 at=-2163.3,-1043.6
            node n1829822457 at=-1975.9,-1265.7
            node n1829822462 at=-2108.7,-995.9
            node n1829822469 at=-1778.9,-1615.8
            node n1831929820 at=-1500.0,-576.0
            node n1831960718 at=-1450.8,-1724.5
            node n1831960739 at=-1427.8,-1700.2
            node n1831960747 at=-1504.1,-1795.7
            node n1831961127 at=-1495.8,-1780.8
            node n1831961135 at=-1540.6,-1735.7
            node n1832238034 at=-93.5,-185.7
            node n1832238040 at=-108.8,-181.5
            node n1832238044 at=-89.0,-171.5
            node n1832238046 at=-104.3,-166.8
            node n1836286636 at=-2058.5,-1051.5
            node n1837448830 at=-1539.1,-645.8
            node n1837448960 at=-1436.3,-531.5
            node n1837448982 at=-1582.9,-552.4
            node n1847853747 at=-897.7,1097.7
            node n1852241862 at=-1953.7,-697.4
            node n1852241866 at=-1975.0,-696.7
            node n1857823513 at=-1542.7,737.5
            node n1904310903 at=286.5,-1587.0
            node n1904310907 at=230.5,-1708.0
            node n1931861630 at=-914.2,-522.6
            node n1947625968 at=-1327.4,-550.4
            node n1951506322 at=-811.4,1.9
            node n1951506336 at=-1859.9,-145.5
            node n1953018747 at=-1318.2,-518.4
            node n1985259401 at=353.3,-1800.2
            node n1985259425 at=678.1,-179.2
            node n2015359296 at=-345.5,1751.8
            node n2082806498 at=734.3,-909.5
            node n2082809510 at=524.0,-1155.8
            node n2519916627 at=1186.3,-189.5
            node n2521023068 at=1441.9,1739.7
            node n2521115159 at=470.3,-1210.9
            node n2521115163 at=597.9,-1079.6
            node n2521115175 at=769.1,-868.2
            node n2521115176 at=689.8,-896.3
            node n2521884269 at=624.2,-1086.3
            node n2521884270 at=761.7,-914.9
            node n2521884271 at=811.0,-854.5
            node n2596273193 at=1151.6,-191.1
            node n2596273195 at=1154.8,-173.2
            node n2598074281 at=-827.2,-1322.8
            node n2598074292 at=-964.4,-924.4
            node n2598074293 at=-910.9,-726.6
            node n2601331947 at=-1912.4,-601.5
            node n2620921746 at=-758.1,-679.9
            node n2653101274 at=983.6,-605.4
            node n2653101281 at=1072.9,-527.6
            node n2750379975 at=-1645.0,-4.5
            node n2750379978 at=-1664.1,0.1
            node n2879240162 at=294.8,-1576.2
            node n2879240165 at=283.2,-1578.5
            node n2879240169 at=258.4,-1574.1
            node n2879240173 at=315.9,-1529.6
            node n2879240177 at=268.3,-1597.5
            node n3010901912 at=-1131.9,277.5
            node n3022208069 at=-934.8,1589.5
            node n3022208072 at=-752.9,1475.3
            node n3059534130 at=-1356.4,-322.7
            node n3059542035 at=-1386.9,-439.0
            node n3059598143 at=-1109.1,-673.2
            node n3071267396 at=-994.4,715.4
            node n3076643620 at=-58.9,855.0
            node n3324524499 at=-1986.0,-1228.1
            node n3324524505 at=-1935.2,-1185.7
            node n3344055036 at=-2100.6,-1032.2
            node n3570841446 at=-1696.9,1315.9
            node n3570841447 at=-1717.2,1340.4
            node n3570841454 at=-1687.3,1349.8
            node n3725683740 at=662.3,-170.9
            node n3756989284 at=-2074.7,-106.3
            node n3756989288 at=-1897.1,6.1
            node n3756989295 at=-2051.7,-23.1
            node n3772851631 at=671.2,-165.6
            node n3772852905 at=669.8,-184.3
            node n3984996251 at=-297.6,-873.5
            node n4242600037 at=-1071.5,-1092.7
            node n4242600038 at=-902.3,-1147.8
            node n4311466610 at=-1532.2,741.6
            node n4451336941 at=-1037.8,253.5
            node n4451336954 at=-1040.0,241.2
            node n4504594293 at=-582.8,1377.8
            node n4598518199 at=-2018.2,-1066.3
            node n4598518203 at=-2029.5,-1069.9
            node n4600634119 at=-2190.1,99.6
            node n4600634120 at=-2179.8,97.2
            node n4600694846 at=-1960.3,-118.0
            node n4600790084 at=-1693.0,-859.5
            node n4600790493 at=-1663.7,-861.6
            node n4600790494 at=-1670.3,-977.4
            node n4600811395 at=-2301.7,-1877.4
            node n4600816516 at=-1674.8,-1709.6
            node n4600842961 at=-2155.4,482.4
            node n4600842962 at=798.9,-925.2
            node n4600842967 at=-1512.6,308.5
            node n4600842969 at=49.2,-203.6
            node n4600842970 at=45.1,-224.4
            node n4600842971 at=70.7,-205.0
            node n4600847758 at=-1498.8,-1178.2
            node n4600854637 at=-235.5,857.5
            node n4600854642 at=-273.1,807.6
            node n4602135005 at=-1214.1,-1614.0
            node n4602135016 at=-1255.5,-1579.0
            node n4602143859 at=-934.7,-1261.3
            node n4602213163 at=261.5,-1613.9
            node n4602213166 at=257.7,-1616.1
            node n4602578367 at=-709.3,1157.2
            node n4602578373 at=-728.8,1163.2
            node n4602578374 at=-856.9,1223.2
            node n4602578375 at=-1158.1,1370.5
            node n4602578377 at=-1036.6,1316.8
            node n4602578383 at=-874.0,1250.8
            node n4602578385 at=-866.6,1239.8
            node n4602578386 at=-1277.7,1420.1
            node n4606287874 at=-2188.9,-88.6
            node n4606287878 at=-2211.3,-48.0
            node n4606287879 at=-2238.8,-192.9
            node n4616343182 at=-1241.2,-159.2
            node n4672154348 at=-1913.4,252.2
            node n4672154362 at=-1529.4,1123.6
            node n4682910150 at=-2069.1,1247.8
            node n4684502273 at=-1470.2,-1684.4
            node n4684627247 at=-2070.9,1274.3
            node n4686999421 at=-1371.7,263.2
            node n4686999423 at=-1389.5,184.2
            node n4686999424 at=-1396.9,177.3
            node n4688852440 at=-1610.0,1156.9
            node n4710801492 at=-1889.1,-1780.6
            node n4710801499 at=-2177.8,-1832.7
            node n4710801500 at=-2047.9,-1777.2
            node n4710862646 at=-2180.7,-1946.4
            node n4710862651 at=-2191.3,-1915.9
            node n4710862656 at=-2067.8,-1653.1
            node n4712021480 at=1034.6,-574.1
            node n4723328877 at=-1950.8,-120.7
            node n4723328880 at=-1888.8,2.7
            node n4723344715 at=-2055.6,-22.2
            node n4723344719 at=-2091.2,-107.2
            node n4724301487 at=-1908.2,-15.6
            node n4724301488 at=-1899.9,-19.2
            node n4724301589 at=-1927.6,-53.7
            node n4724301590 at=-1919.2,-57.4
            node n4724301591 at=-1938.4,-74.9
            node n4724301592 at=-1930.0,-78.6
            node n4724301593 at=-1946.4,-90.6
            node n4724301594 at=-1937.9,-94.2
            node n4724570673 at=-710.2,-482.3
            node n4724570677 at=-759.0,-604.7
            node n4724612488 at=-837.5,-221.1
            node n4724923624 at=-1106.9,-1211.8
            node n4724923625 at=-1221.0,-1167.1
            node n4724923626 at=-1363.8,-1081.1
            node n4724923628 at=-1254.1,-1148.0
            node n4726227141 at=-195.1,824.8
            node n4726412914 at=-1004.7,748.6
            node n4726775783 at=-817.5,622.0
            node n4726775785 at=-847.0,606.0
            node n4729974845 at=-1272.4,-1137.6
            node n4732312054 at=-1385.4,1466.4
            node n4732339357 at=-240.9,909.8
            node n4732339359 at=-248.2,899.5
            node n4732407688 at=-871.5,1180.9
            node n4732525705 at=-1103.0,531.7
            node n4732525709 at=-857.2,1261.0
            node n4734606540 at=-2131.3,672.1
            node n4734606543 at=-2121.2,684.3
            node n4736504887 at=-2284.5,-939.0
            node n4736508655 at=-2295.0,-1325.4
            node n4738400701 at=-1226.2,1251.8
            node n4739815319 at=-1918.4,-615.6
            node n4744279229 at=-1875.0,-623.2
            node n4744279230 at=-1875.8,-626.5
            node n4745106066 at=-1827.1,-1530.6
            node n4745106067 at=-1838.7,-1536.6
            node n4745106068 at=-1639.7,-1206.2
            node n4745106074 at=-1604.6,-1516.5
            node n4745106075 at=-1590.5,-1562.2
            node n4745106078 at=-1619.7,-1386.2
            node n4745151601 at=-1728.6,-993.7
            node n4745192776 at=-1608.6,-1385.5
            node n4745192778 at=-1541.8,-1379.4
            node n4745192782 at=-1615.2,-1318.6
            node n4745272381 at=-1516.3,-974.8
            node n4745272383 at=-1584.6,-923.6
            node n4745272384 at=-1579.5,-914.9
            node n4745441753 at=-1519.7,-990.8
            node n4745441756 at=-1507.6,-980.8
            node n4982866573 at=-1501.8,-973.5
            node n4982866591 at=-1543.5,-1707.6
            node n4982866593 at=-1579.1,-1558.2
            node n4982866863 at=-1485.2,-1300.9
            node n4982867037 at=-1661.6,-975.7
            node n4982867047 at=-1644.6,-1035.2
            node n4982867053 at=-1632.6,-1204.7
            node n5006326665 at=204.8,-1697.2
            node n5006326669 at=211.0,-1684.4
            node n5006326670 at=210.7,-1671.5
            node n5006326688 at=610.5,-1110.1
            node n5006326691 at=29.9,-1489.4
            node n5006326693 at=156.2,-1470.2
            node n5069323959 at=1002.3,-486.3
            node n5094607351 at=-1076.3,-898.1
            node n5138586950 at=-2380.4,-1403.3
            node n5138586955 at=-2042.3,-1775.1
            node n5138586956 at=-2044.1,-1768.4
            node n5165895530 at=-1829.5,-417.2
            node n5165895535 at=-1855.4,-419.8
            node n5165925965 at=-194.7,622.7
            node n5172094036 at=-2083.7,-933.8
            node n5172094047 at=-2312.1,-927.1
            node n5186902946 at=-2002.7,139.1
            node n5187147003 at=-2285.8,-790.5
            node n5187250342 at=-1551.5,263.0
            node n5187331770 at=-315.1,-934.4
            node n5187405194 at=1003.4,-658.1
            node n5231423068 at=-1930.2,-828.0
            node n5233472026 at=-1515.0,-556.7
            node n5233472032 at=-1368.7,-1073.4
            node n5233472037 at=-1301.9,-1018.3
            node n5233582710 at=-1474.6,726.6
            node n5233639468 at=-2058.8,-180.2
            node n5233639475 at=-2044.0,-19.8
            node n5243406658 at=-185.6,1483.0
            node n5243492221 at=-1378.1,1463.2
            node n5243492222 at=-1385.5,1448.6
            node n5243492223 at=-1393.8,1437.2
            node n5270952271 at=-889.4,-50.9
            node n5278629766 at=270.6,1682.2
            node n5282854973 at=-683.1,-489.9
            node n5282930777 at=-1260.9,-1536.3
            node n5282930784 at=-1230.4,-1517.2
            node n5432103372 at=-2024.0,443.6
            node n5600204982 at=-1343.2,-269.7
            node n5600204988 at=-1351.9,-324.5
            node n5600204989 at=-1338.9,-270.7
            node n5600204998 at=-1389.9,-438.3
            node n5686639667 at=-1703.1,-984.5
            node n5765341406 at=-1611.2,1210.9
            node n5794881171 at=-2044.2,451.0
            node n5809892935 at=-1667.6,-822.0
            node n5817288340 at=-1171.0,-872.2
            node n5817301660 at=-1169.6,-702.0
            node n5817301661 at=-1120.9,-713.8
            node n6038493817 at=-1687.0,-980.6
            node n6048295590 at=-1765.0,-940.9
            node n6064305393 at=1291.9,1718.0
            node n6094644741 at=538.7,-851.4
            node n6138767692 at=865.7,-802.5
            node n6151700895 at=-2265.6,1007.4
            node n6228532829 at=-1479.5,-1522.3
            node n6232047026 at=-2316.9,534.2
            node n6232047028 at=-2316.2,511.8
            node n6246027973 at=-969.2,1500.1
            node n6246027974 at=-796.3,1398.0
            node n6306233270 at=556.2,-856.7
            node n6309963192 at=-51.5,482.6
            node n6353451366 at=-966.9,-933.2
            node n6365520408 at=-1327.0,-848.2
            node n6408411362 at=-1400.8,-486.6
            node n6413515288 at=-750.3,737.2
            node n6528381379 at=422.6,-1734.2
            node n6528381380 at=420.4,-1731.3
            node n6528381381 at=360.1,-1803.2
            node n6536886453 at=-2163.8,-1467.3
            node n6552271865 at=-1393.7,-500.9
            node n6575109847 at=-1491.7,-1801.2
            node n6639134876 at=-224.5,858.1
            node n6691407335 at=-1540.3,-1381.3
            node n6691407336 at=-1539.4,-1379.3
            node n6691407338 at=-1543.7,-1358.4
            node n6703954131 at=-2367.0,-1405.2
            node n6749812859 at=219.2,1263.5
            node n6856578454 at=-1178.1,295.1
            node n6856578455 at=-1156.5,286.8
            node n6897440292 at=-1460.5,-1691.4
            node n6897440293 at=-1452.1,-1707.7
            node n6993992144 at=-983.3,-402.7
            node n7008336140 at=-1340.1,-1423.5
            node n7012974711 at=-1497.8,-981.8
            node n7016947288 at=-151.0,589.8
            node n7027603394 at=-1836.5,-635.0
            node n7027603403 at=-1933.4,-597.9
            node n7027677929 at=-1325.5,-836.0
            node n7030292675 at=-1760.1,-1207.0
            node n7030292676 at=-1884.9,-1108.1
            node n7030663129 at=-1818.6,-1164.3
            node n7030663130 at=-1817.1,-1162.4
            node n7030663131 at=-1837.8,-1148.0
            node n7030663132 at=-1836.3,-1146.1
            node n7043034841 at=-2071.6,1283.6
            node n7043034842 at=-1875.7,1183.3
            node n7043034847 at=-1863.1,1143.5
            node n7094958410 at=-1377.2,259.6
            node n7097470158 at=-1374.0,-1097.8
            node n7097470164 at=-1348.8,-1089.6
            node n7118869386 at=-2311.9,-1318.9
            node n7152969479 at=-1445.6,720.2
            node n7153058636 at=2275.2,1691.6
            node n7153058639 at=2270.1,1684.6
            node n7229619775 at=-1559.6,622.8
            node n7241390520 at=-1562.2,-1219.2
            node n7271112667 at=-1331.1,-836.1
            node n7705372941 at=-1584.9,-911.4
            node n7705372942 at=-1590.4,-919.9
            node n7705372943 at=-1542.7,-860.4
            node n7828120483 at=-1259.3,-1024.6
            node n7852537777 at=-2371.4,-889.8
            node n7904178876 at=-1421.4,82.8
            node n8061280812 at=-2334.9,-1413.9
            node n8133473091 at=512.3,-1062.7
            node n8153290963 at=-1726.1,-898.7
            node n8167517153 at=-2371.6,601.0
            node n8167517154 at=-2377.0,595.8
            node n8229495948 at=1022.6,1735.3
            node n8869838485 at=-265.1,-1309.0
            node n8875654200 at=-1016.9,-1109.0
            node n9350542650 at=-1142.5,-1153.4
            node n9350542655 at=-1137.1,-1128.7
            node n9370421301 at=-1075.3,257.9
            node n9489438156 at=-1026.8,-922.9
            node n9489438159 at=-1022.3,-905.6
            node n9559521797 at=-2327.9,-904.0
            node n9559558762 at=-1112.1,-304.3
            node n9913888157 at=-567.8,-1048.8
            node n9913888161 at=-752.9,-995.2
            node n9913888162 at=-843.5,-968.9
            node n9913888164 at=-217.1,-1150.4
            node n9987737918 at=-2264.3,-1587.4
            node n9987737920 at=-2266.3,-1584.3
            node n9987737922 at=-2269.6,-1579.1
            node n9987737924 at=-2275.8,-1590.9
            node n10765152292 at=-2273.2,-753.5
            node n11258902323 at=407.9,1506.0
            node n11259104759 at=-812.3,71.5
            node n11359537763 at=-2051.3,439.1
            node n11376069516 at=403.0,1515.6
            node n11376108181 at=578.7,1713.7
            node n11376108182 at=580.2,1708.7
            node n11389450853 at=-365.0,-1097.4
            node n11389450854 at=-386.4,-1101.4
            node n11389450855 at=-383.3,-1091.8
            node n11424995020 at=1020.4,1759.7
            node n11425144215 at=1038.1,1751.0
            node n11465244435 at=679.9,-391.9
            node n11465244446 at=689.0,-396.4
            node n12071463879 at=-879.8,651.8
            node n12071463880 at=-866.0,607.9
            node n12615243863 at=822.8,-434.7
            node n12615243873 at=677.1,-400.6
            node n12615243874 at=685.9,-404.5
            node n12687053274 at=-311.8,1265.8
            node n12741088964 at=-350.2,1286.8
            node n13064657905 at=277.5,-1741.9
            node n13698051636 at=20.4,845.5
            node n13770297447 at=519.6,-1822.9
            node n13770297452 at=471.4,-1880.6
            node n13770297457 at=392.9,-1818.5
            node n13770297460 at=438.7,-1757.3
            node n13812791644 at=-834.3,-21.5
            node n13917491481 at=-2061.0,65.9
            node n13987570565 at=-1327.3,-1395.6
            node n14250610564 at=-1910.5,-40.2
            node n14250610565 at=-1918.6,-36.1
            road "Unity Street" kind=residential-2
              from n74405854
              to n471501777
              to n74405856
            road "Merchant Loop one way" kind=residential-1
              from n74444085
              to n74444087
            road "Clemenceau Avenue one way" kind=primary-2
              from n237296882
              to n571492385
              to -2208.0,14.9
              to n4600634119 kind=primary-3
              to n74460234
              to -2183.4,137.1 kind=primary-2
              to n74454947 bridge
              to -2167.9,221.9 kind=primary-4
              to n74405854 ground
              to n74454952
              to n612392126
              to n612392154 kind=primary-3
              to -2095.6,551.3
              to -2097.3,580.7
              to -2105.0,610.5
              to -2126.4,661.1
              to n4734606540 kind=primary-4
            road "Prinsep Street one way" kind=primary-4
              from n235130001
              to -1762.3,1123.3
              to -1766.9,1133.3
              to -1768.7,1147.8
              to -1767.0,1154.6
              to n233497138
              to n3570841446
              to n3570841454
            road "Nicoll Highway one way" kind=trunk-4
              from n233879753
              to -862.3,659.6
              to -838.4,771.6
              to n172510077 kind=trunk-5
              to n1683121556 kind=trunk-3
              to -785.1,960.9 kind=trunk-4
              to -771.9,1009.5 kind=trunk-3
              to -760.7,1050.8 kind=trunk-4
              to -747.4,1099.1 kind=trunk-3
              to n233783297 kind=trunk-4
              to n4602578373 kind=trunk-3
              to -692.3,1251.7
              to -654.8,1327.7
              to -569.3,1479.4
              to -532.2,1536.4
              to -487.4,1597.7
              to -398.7,1710.6
              to -353.4,1770.9
            road "Hill Street one way" kind=primary-3
              from n233421104
              to -1445.3,673.2
              to -1451.8,627.7 kind=primary-4
              to n232207249
              to -1455.3,566.7 kind=primary-3
              to -1461.6,519.9
              to n232207435 kind=primary-4
              to -1492.2,364.5
              to n4600842967
              to n237170319
            road "High Street one way" kind=tertiary-2
              from n232207661
              to -1493.5,409.0
              to -1488.6,411.4
              to n232207435
              to -1355.9,365.4 kind=tertiary-3
              to n232147366
            road "Hill Street one way 2" kind=primary-4
              from n232207994
              to n74442036
              to -1515.7,344.4 kind=primary-5
              to -1510.5,363.9
              to n232207661 kind=primary-6
              to -1476.6,524.7 kind=primary-4
              to n232207771
              to -1455.9,675.0
              to n7152969479
              to -1434.1,780.8 kind=primary-5
              to n232207810
            road "North Boat Quay one way" kind=tertiary-2
              from n7094958410
              to -1447.8,281.5
              to -1478.9,289.6 kind=tertiary-3
              to n237170319
            road "Coleman Street one way" kind=tertiary-2
              from n232353783
              to -1193.9,532.1
              to -1185.1,532.8
              to -1126.9,519.4
              to -1113.4,523.5
              to n4732525705
            road "Stamford Road one way" kind=primary-5
              from n233879421
              to n233879753
              to n12071463879 kind=primary-4
              to -970.5,702.4
              to n3071267396 kind=primary-5
              to n232353226 kind=primary-4
              to n232376514
              to -1180.0,770.3 kind=primary-5
              to -1213.5,776.2
              to n232147312
              to -1335.7,813.6 kind=primary-4
              to n233421104
              to n232207810
              to n232353329
              to -1499.3,914.5 kind=primary-3
              to n1408671492 kind=primary-4
              to -1496.9,940.6 kind=primary-3
              to -1498.6,951.3
              to -1505.2,962.5
              to -1532.3,976.6
              to -1577.3,994.4
              to -1581.7,1000.0
              to -1601.2,1014.1
              to -1630.2,1039.5
              to -1660.1,1064.7 kind=primary-5
              to n235129976
            road "primary_link one way" kind=primary-link-3
              from n232376467
              to -1068.5,707.0
              to -1076.8,725.4
              to n232376514
            road "Victoria Street one way" kind=primary-4
              from n232207810
              to -1384.6,916.7
              to n233432379
              to -1267.3,1153.4
              to n4738400701
              to -1199.2,1316.9
              to n233432515 kind=primary-5
              to n233432542 kind=primary-3
              to n595267994 kind=primary-4
              to n595268007
              to -1088.2,1567.9
            road "Selegie Road one way" kind=primary-4
              from n3570841454
              to -1685.6,1396.8
            road "Bain Street one way" kind=residential-1
              from n4738400701
              to n206833694
              to -1199.9,1242.1
              to -1162.5,1228.4 kind=residential-2
              to n233602622
            road "Seah Street one way" kind=residential-1
              from n1847853747
              to -907.7,1089.7
              to n232082494
            road "Raffles Link" kind=residential-2
              from n233879440
              to -741.8,594.9
              to -751.0,616.3
              to -752.8,627.9
              to -750.7,658.8
              to n6413515288
            road "Temasek Boulevard one way" kind=tertiary-2
              from n233880373
              to -619.4,826.6
              to -612.2,835.7
              to -554.4,1063.5
              to -554.9,1073.3
              to n1130779944
            road "Temasek Boulevard one way 2" kind=tertiary-2
              from n1271162085
              to -529.1,1169.6
              to -527.4,1174.0
              to -514.6,1228.4
              to -502.9,1266.4
              to -496.9,1309.4
              to -502.9,1328.2
              to n233782664
            road "trunk_link one way" kind=trunk-link-2
              from n234839820
              to -55.1,891.4 bridge
              to -67.1,776.0
              to -68.1,720.8
              to -64.0,674.9
              to -55.6,622.3
              to -48.1,585.8
              to -36.6,533.2 kind=trunk-link-1
              to n1145435573
            road "motorway_link one way" kind=motorway-link-2
              from n12687053274
              to -302.9,1263.8
              to -290.9,1256.2
              to -264.3,1229.3
              to -236.5,1212.2
              to -202.7,1188.0 bridge
              to -174.7,1175.5
              to -143.5,1168.5
              to -111.2,1165.4
              to -75.6,1168.4
              to -39.7,1176.9
              to 64.1,1214.8
              to n26782016
            road "Republic Boulevard one way" kind=primary-3
              from n235071053
              to n1538307293
            road "Fort Canning Link one way" kind=primary-3
              from n232353329
              to -1578.9,874.6
            road "Waterloo Street" kind=residential-3
              from n5243492223
              to -1425.1,1364.2
              to -1456.1,1293.6 kind=residential-2
              to n4672154362 kind=residential-3
            road "Orchard Road one way" kind=primary-4
              from n235129976
              to n1818199237
              to n235130001
              to -1783.9,1119.7 kind=primary-2
              to -1843.4,1145.3 kind=primary-3
              to -1853.6,1146.2
              to n7043034847
              to n236429505
            road "Penang Road one way" kind=primary-2
              from n236429505
              to -1888.0,1129.5
              to -1910.5,1091.1
              to -1929.6,1073.2
              to -1950.8,1064.7
              to n235180430
              to -2055.9,1065.1 kind=primary-5
              to n408999681
              to -2211.1,1067.7
              to n1275666711 kind=primary-6
              to n74454975 kind=primary-5
              to -2355.2,1098.9
              to n409000687 kind=primary-6
            road "Robinson Road one way" kind=trunk-4
              from n237055115
              to -1191.6,-1125.2
              to -1177.5,-1091.0
              to -1164.5,-1040.8
              to n237055163 kind=trunk-5
              to n241595237
              to n246759045
              to -1021.4,-560.4 kind=trunk-4
              to n246817239
              to -980.1,-431.8
              to -977.0,-422.4
              to n1633162341
            road "Telok Ayer Street" kind=residential-2
              from n5233472037
              to -1305.8,-1013.3
              to -1311.1,-1001.4
              to -1317.7,-982.0
              to -1325.2,-956.0
              to -1327.8,-940.1
              to -1331.3,-909.2
              to n6365520408
            road "Wallich Street one way" kind=residential-2
              from n4729974845
              to -1323.4,-1164.2
              to -1335.4,-1165.9
              to -1341.4,-1164.5
              to -1345.3,-1160.6
              to -1353.4,-1140.7
              to -1358.9,-1132.7
              to n241595238
            road "Choon Guan Street one way" kind=residential-2
              from n4982866863
              to -1389.3,-1265.1
              to -1289.1,-1223.8
              to -1264.7,-1211.1
              to n1318544672
            road "Cook Street one way" kind=residential-1
              from n1814187447
              to n4982867047
              to n237105802
            road "Anson Road one way" kind=trunk-2
              from n6897440292
              to n237154345
              to -1415.0,-1596.1 kind=trunk-4
              to n237154414
              to n7008336140
              to n13987570565
              to -1300.2,-1331.9
              to n237154438 kind=trunk-5
              to n1318544653
              to -1236.4,-1190.8
              to n4724923625
              to n237055115
            road "Hoe Chiang Road one way" kind=tertiary-3
              from n4982866593
              to n4745106075
              to -1748.4,-1619.1
              to n237154569
            road "New Bridge Road one way" kind=primary-4
              from n237170319
              to -1522.4,285.4
              to -1550.2,216.4 bridge
              to n237170343 ground
              to n237170369
              to -1622.4,46.4
              to n237218543 kind=primary-5
              to n2750379975 kind=primary-6
              to n237170380 kind=primary-5
              to n237170391
              to n1629037383 kind=primary-4
              to n237170487 kind=primary-3
              to n237170508 kind=primary-4
              to -1784.3,-318.1
              to -1787.2,-325.2 kind=primary-5
              to n237170538 kind=primary-6
              to n5165895530 kind=primary-4
              to n246800416
              to n246800441
              to n2601331947
              to n4739815319
              to n237170655
              to n1852241862 kind=primary-5
            road "Hongkong Street one way" kind=residential-1
              from n237218629
              to -1457.6,-24.5
              to -1463.6,-24.8
              to -1593.3,8.8
              to n237218543
            road "North Canal Road one way" kind=secondary-3
              from n74460245
              to n237170380
              to -1623.9,-71.1 kind=tertiary-2
              to n237012110
              to n237219274
              to n246806310
            road "South Canal Road one way" kind=tertiary-4
              from n237251040
              to n237218667
              to -1600.2,-85.6 kind=tertiary-3
              to n237170391 kind=tertiary-4
              to n237218756 kind=secondary-4
            road "Cantonment Road one way" kind=primary-4
              from n237218779
              to n237170711
              to -2090.7,-1116.7 kind=primary-3
              to -2046.6,-1163.4
              to n3324524499 kind=primary-4
              to -1968.3,-1251.2 kind=primary-3
              to -1960.1,-1264.8
              to -1911.1,-1362.9
              to -1897.6,-1389.5 kind=primary-4
              to n4745106066 kind=primary-3
            road "Upper Hokien Street one way" kind=residential-2
              from n246806298
              to n237170508
            road "Outram Road one way" kind=primary-5
              from n237348779
              to n237348741
              to n1829822456 kind=primary-4
              to -2203.2,-995.7 kind=primary-3
              to -2219.4,-980.4
              to -2240.8,-963.1
              to n4736504887
              to n5172094047
              to -2329.7,-921.0
            road "motorway_link one way 2" kind=motorway-link-1
              from n1363293957
              to -2062.4,-160.1
              to n1363294010
            road "Fort Canning Road one way" kind=secondary-2
              from n235129976
              to -1686.9,1072.8
              to -1698.4,1069.1
              to -1718.8,1065.4
              to n243413548
              to -1709.2,1073.1 kind=secondary-1
              to -1705.0,1076.9
              to -1703.1,1081.3
              to n1818199237
            road "Keong Saik Road" kind=residential-2
              from n243668561
              to -1934.8,-990.4
              to -1939.7,-956.7
              to n243668563
              to n1629037257
            road "Keong Saik Road one way" kind=residential-1
              from n1629037271
              to -1934.3,-814.1
              to n243668564
            road "Bukit Pasoh Road one way" kind=residential-1
              from n3324524505
              to -1959.0,-1158.6
              to -1995.4,-1112.9
              to -2006.9,-1097.5 kind=residential-2
              to n243668569
            road "Lorong Telok one way" kind=residential-1
              from n237219274
              to -1374.4,-25.2
              to n246754375 kind=residential-2
            road "Fullerton Road one way" kind=trunk-4
              from n237054576
              to -810.3,47.2
              to n1951506322
              to -828.5,-122.2
              to n4724612488
            road "Jiak Chuan Road" kind=residential-2
              from n243668561
              to -1917.5,-1016.7
              to n246800493
            road "Duxton Hill" kind=residential-2
              from n6038493817
              to n5686639667
            road "Everton Road" kind=residential-2
              from n243210137
              to n246801947
              to n6536886453
              to n1829436720
            road "Everton Road one way" kind=residential-1
              from n246801944
              to -2233.9,-1466.5
              to n246801947
            road "Stanley Street one way" kind=residential-1
              from n246801955
              to n246801952
            road "motorway_link one way 3" kind=motorway-link-2
              from n237154323
              to -1258.7,-1620.2
              to n4602135005
            road "Yan Kit Road" kind=residential-2
              from n246806266
              to -1723.9,-1262.6
              to n246800861
            road "residential" kind=residential-2
              from n5270952271
              to -893.2,-41.7
              to -938.7,7.5
              to -943.7,27.3
            road "Malacca Street one way" kind=residential-3
              from n246810583
              to -1013.3,-290.2
              to -1077.6,-303.1
              to -1105.8,-307.9 kind=residential-2
              to -1109.4,-307.0
              to n9559558762
            road "Phillip Street one way" kind=unclassified-1
              from n246810582
              to -1194.2,-178.6
              to -1196.9,-185.3
              to -1196.1,-260.8
              to -1196.5,-287.3 kind=unclassified-2
              to n246810548
            road "Shenton Way one way" kind=trunk-5
              from n240002104
              to -986.8,-777.5
              to n9489438159 kind=trunk-6
              to n9489438156 kind=trunk-5
              to -1069.3,-1085.2
              to n4242600037 kind=trunk-6
              to n237055087
              to n4724923624 kind=trunk-5
              to -1123.6,-1259.9
              to n237154196
              to -1154.5,-1357.8 kind=trunk-4
              to n246806248 kind=trunk-5
              to n5282930784
            road "Finlayson Green one way" kind=primary-3
              from n246800406
              to -1023.2,-419.9
              to n1633162341
              to n237054972 kind=primary-7
            road "Marina View Link one way" kind=unclassified-1
              from n248536068
              to -747.5,-613.4
              to n4724570677
              to -808.7,-587.9 kind=unclassified-2
              to -817.1,-582.3
              to -826.0,-567.9
              to -827.5,-558.0
              to -826.0,-550.0
              to -811.7,-500.0
              to -800.7,-473.3
              to n248527325
            road "Bayfront Avenue one way" kind=secondary-2
              from n11389450854
              to n11389450855
              to n1539897226
              to -254.1,-674.3 kind=secondary-3
              to n1539883491 kind=secondary-4
              to -216.3,-536.9 kind=secondary-3
              to -139.5,-289.5
              to -126.7,-243.2
              to n1832238040 kind=secondary-4
              to n1832238046 kind=secondary-3
              to -75.4,-53.0
              to -50.7,36.9
              to -45.2,84.9
              to -43.9,137.6 kind=secondary-4
              to -49.9,187.4 kind=secondary-3
              to -52.5,198.0
              to n7016947288 bridge
              to n479983003 kind=secondary-2 ground
            road "Gopeng Street" kind=residential-1
              from n4745106078
              to n4745192776
              to n4745192778 kind=residential-2
              to n6691407336
              to n237105451
              to -1423.2,-1363.4
              to -1408.6,-1364.2
              to n13987570565
            road "Upper Pickering Street one way" kind=primary-5
              from n237219180
              to n237170487
              to n1629037351
              to -1659.8,-197.3 kind=primary-6
              to n237012130 kind=primary-5
            road "Pickering Street one way" kind=primary-5
              from n237012130
              to n246806302
            road "Chin Swee Road one way" kind=primary-3
              from n1790124104
              to n1790098242
              to -2228.6,-157.9 kind=primary-2
              to n249692339
              to n4606287879
            road "Bonham Street" kind=residential-2
              from n249766690
              to -1047.6,-79.8
            road "Flint Street" kind=residential-2
              at -962.3,-19.5
              to -937.8,-48.2
              to -935.5,-55.6
              to n249766692
            road "Marina Grove" kind=unclassified-2
              from n2653101281
              to 1043.3,-504.9
              to 1028.4,-495.8
              to n5069323959
            road "Second Hospital Avenue" kind=residential-2
              from n249792083
              to -2323.6,-1007.8
              to -2347.8,-1051.8
              to -2356.9,-1097.5
              to -2359.3,-1139.0
              to -2355.6,-1146.8
            road "Central Expressway one way" kind=motorway-3
              at -2304.5,-280.0
              to -2371.7,-349.2
            road "South Bridge Road one way" kind=secondary-2
              from n323137286
              to n4686999424
            road "Ann Siang Hill one way" kind=residential-1
              from n246806296
              to -1581.2,-704.7
              to n249690363
            road "Pearl's Hill Road one way" kind=residential-1
              from n237296580
              to -2069.5,-237.5
              to -2078.2,-232.6
              to -2084.8,-231.6
              to n1363294008
              to -2070.0,-212.1
              to n237296663
            road "motorway_link one way 4" kind=motorway-link-1
              from n6064305393
              to 1277.1,1707.6
              to 1189.7,1674.5
              to 1164.1,1660.0
              to 1147.4,1645.6
              to 1137.9,1634.6
              to 1130.6,1623.1
              to 1122.9,1605.2
              to 1120.3,1591.0
              to 1120.4,1576.9
              to 1122.8,1567.1
              to 1130.5,1551.3
              to 1136.9,1542.6
              to 1155.1,1529.1
              to 1174.1,1523.3
              to 1182.7,1522.4
              to 1199.4,1525.2
              to 1208.8,1528.1
              to 1224.2,1536.9
              to 1233.0,1544.2
              to 1238.7,1551.3
              to 1248.7,1569.1
              to 1253.0,1582.8
            road "Keppel Road one way" kind=trunk-4
              from n246806211
              to -1325.6,-1669.3
              to -1404.7,-1692.7
              to n1831960739 kind=trunk-5
              to n6897440293 kind=trunk-4
              to n246806260
              to n1831961135 kind=trunk-3
              to n388569725
              to -1693.5,-1786.4
              to n388569724 kind=trunk-4
              to n4710862646 kind=trunk-3
            road "Zubir Said Drive" kind=residential-2
              at -1824.0,1283.1
              to -1813.0,1238.3
              to n240686973
            road "Penang Lane one way" kind=tertiary-2
              from n408999675
              to -2193.8,1168.4
              to -2187.7,1161.4
              to -2184.5,1147.1 kind=tertiary-3
              to n408999681
            road "Buyong Road one way" kind=tertiary-5
              from n409000687
              to -2395.8,1117.6
              to -2399.2,1124.1
            road "Oxley Rise one way" kind=secondary-2
              from n243234023
              to -2139.6,865.2 bridge
              to -2151.3,853.7
              to -2164.3,844.8
              to -2174.1,840.2
              to -2197.9,832.5
              to -2220.2,831.5
              to -2230.6,832.9
              to -2247.6,837.8
              to -2263.3,845.5
              to n1363263545 kind=secondary-3 ground
              to -2328.8,906.4 kind=secondary-4
              to n1363277802 kind=secondary-3
              to -2394.0,978.0
            road "Fort Canning Link one way 2" kind=primary-3
              at -1952.0,1041.7
              to -1988.5,1055.3
              to n235180430
            road "Mistri Road one way" kind=residential-2
              from n554037746
              to -1277.3,-1522.3
              to -1275.6,-1510.8
              to -1263.8,-1485.1
              to n554037752
            road "Boon Tat Link" kind=residential-2
              from n555760573
              to -1052.7,-818.2
              to -1070.5,-880.1 kind=residential-1
              to -1075.4,-894.9 kind=residential-2
              to n5094607351 kind=residential-1
            road "New Market Road one way" kind=residential-3
              at -1856.5,-209.8
              to n597413159
              to n564553552 kind=residential-2
            road "motorway_link one way 5" kind=motorway-link-1
              from n237251482
              to -1806.3,-23.3
              to -1929.0,30.1
              to -1967.1,45.7
              to -1998.7,55.4
            road "primary_link one way 2" kind=primary-link-1
              from n249692307
              to -2245.2,-138.8
              to -2249.7,-127.2
              to -2264.0,-109.3
              to n243213817
            road "primary_link one way 3" kind=primary-link-1
              from n571492380
              to -2250.5,-65.5
              to -2237.6,-59.6
              to -2225.7,-47.0
              to n571492385
            road "Central Expressway one way 2" kind=motorway-link-2
              at -2137.6,-121.0
              to -2120.7,-128.2
              to -2103.7,-130.8
              to -2043.0,-121.3
              to n243208171
            road "Central Expressway one way 3" kind=motorway-link-1
              at -2059.1,83.1
              to -1952.3,49.6
              to -1860.8,9.2
              to n1790324627
            road "Penang Lane one way 2" kind=unclassified-1
              from n1275666709
              to -2189.9,862.9
              to -2180.6,860.0
              to -2172.7,859.6
              to -2164.6,862.7
              to -2159.6,866.7
              to -2152.0,878.9
              to -2150.8,883.3 kind=unclassified-2
              to -2154.3,898.0
              to -2182.9,975.1
              to -2192.2,1004.9
              to n408999681
            """;

    private static final String PART_2 = """
            road "Manila Street" kind=residential-2
              from n595267991
              to n595267994
            road "Temasek Avenue one way" kind=secondary-2
              from n479983003
              to n233879872
              to n4600854637 kind=tertiary-1
              to n4732339359
              to -256.4,927.0
              to -273.8,980.4 kind=tertiary-2
              to -291.7,1028.4
              to -298.4,1041.0
              to -307.3,1051.9 kind=tertiary-3
              to -313.3,1056.7
              to -321.3,1062.9
              to -330.2,1066.1
              to n595497898 kind=tertiary-2
            road "Rochor Road one way" kind=trunk-4
              from n4504594293
              to -655.0,1416.6 bridge
              to -689.0,1436.6 ground
              to n232083643 kind=trunk-6
              to n3022208072 kind=trunk-4
              to -892.2,1566.9 kind=trunk-5
              to n3022208069
              to -1035.7,1647.5
            road "Smith Street one way" kind=residential-1
              from n2601331947
              to -1911.7,-614.0
              to n4744279229
              to -1837.4,-633.4
              to n7027603394
            road "Kee Seng Street" kind=residential-2
              at -1732.1,-1526.5
              to -1644.9,-1518.1
              to n4745106074
              to n237153674
            road "McCallum Street one way" kind=residential-3
              from n7027677929
              to -1314.9,-836.9
              to n246801955
              to n237089541
            road "Tank Road" kind=residential-2
              at -2192.0,761.9
              to -2185.2,763.0
              to -2182.6,762.0
              to n1790366566
            road "Ophir Road one way" kind=trunk-2
              from n12741088964
              to -345.2,1285.7
              to -325.3,1272.8
              to n12687053274 kind=trunk-4
            road "Cantonment Road one way 2" kind=primary-3
              from n237154569
              to n1829822448
              to -1730.2,-1654.2 kind=primary-4
              to -1718.9,-1669.3
              to n4600816516
            road "Bayfront Avenue one way 2" kind=secondary-3
              from n233879603
              to -139.3,593.9
              to -46.8,236.9 bridge
              to -37.6,201.6 kind=secondary-4
              to -29.2,138.5 ground
              to -30.1,81.0 kind=secondary-3
              to -35.2,34.9
              to -46.6,-10.4
              to -54.6,-35.9
              to -74.0,-104.9 kind=secondary-4
              to n1832238044 kind=secondary-3
              to n1832238034
              to -125.8,-293.9
              to -144.5,-352.5 kind=secondary-4
              to -176.4,-461.3 kind=secondary-3
              to -199.3,-533.3
              to n1539883582
              to -262.9,-758.2
              to n1539897224 kind=secondary-4
              to n3984996251 kind=secondary-2
            road "Temasek Boulevard one way 3" kind=tertiary-2
              from n1363236276
              to -536.7,1068.1
              to -541.5,1061.0
              to n595497900
              to -597.8,844.0
              to -597.8,832.2
              to -593.4,819.2
              to n595497945
            road "Fountain of Wealth" kind=tertiary-3
              from n1130779944
              to -572.6,1098.6
              to -576.1,1106.5
              to -578.2,1121.9
              to -575.5,1135.3
              to -570.3,1145.2
              to -564.1,1152.2
              to -556.6,1157.8
              to n1271162085
              to -531.4,1163.8
              to -516.6,1160.3
              to n1363236253
              to -495.6,1141.2
              to -492.6,1134.3
              to n408733173
              to -493.4,1104.6
              to n1130779933
              to -514.0,1081.5
              to n1363236276
              to -528.5,1076.9
              to -543.0,1077.3
              to -556.6,1082.5
              to n1130779944
            road "motorway_link one way 6" kind=motorway-link-2
              from n6749812859
              to 156.4,1221.6 bridge
              to 87.7,1182.2
              to 55.7,1161.9
              to 31.5,1143.3
              to 8.9,1121.3
              to -13.0,1095.2
              to -47.9,1035.1
              to -61.1,1017.7
              to -73.5,1006.9
              to -89.7,996.6
              to -106.9,989.1
              to -134.2,985.1
              to -149.4,986.4
              to -173.2,992.0 ground
              to -193.1,1003.3
              to -209.2,1018.4
              to -276.4,1115.7
              to n1145428485
            road "Clemenceau Avenue one way 2" kind=primary-2
              from n1275666711
              to -2232.1,1067.0
              to -2249.7,1063.0
              to -2254.6,1059.1
              to -2259.1,1052.7
              to -2262.0,1041.7
              to -2261.3,1036.1 kind=primary-3
              to n1275666710
            road "Cecil Street one way" kind=secondary-4
              from n1318544653
              to n1318544672
              to -1250.0,-1183.0
              to n4724923628
              to n1318544659 kind=secondary-3
              to -1270.1,-1095.6 kind=secondary-4
              to n1318544675
              to n237089604
              to n7828120483
              to -1242.8,-963.7
              to n237089541
              to n5817301660
              to n246801953
              to n26782230
              to -1120.2,-517.2
              to n246810673 kind=secondary-5
              to -1092.5,-445.4 kind=secondary-4
              to -1083.3,-435.4
              to n246810545
              to n246800406 kind=primary-5
            road "Cantonment Link one way" kind=residential-2
              from n1318603823
              to -1796.2,-1644.5
              to -1794.0,-1620.2
              to -1794.6,-1613.9
              to n1318603811
            road "Queen Street" kind=tertiary-4
              from n4602578386
              to n233801197
              to -1293.9,1381.2
              to -1322.5,1316.3 kind=tertiary-3
              to -1341.4,1267.4 kind=tertiary-2
              to -1362.2,1219.5
              to -1374.6,1203.0
              to -1402.2,1140.8
              to n410055846
            road "Jalan Rumbia" kind=residential-2
              at -2258.9,718.9
              to -2258.9,789.5
              to -2260.7,806.1
              to n1363263232
            road "Tank Road one way" kind=residential-1
              from n1363263536
              to -2172.9,710.4
              to n1363263413
            road "Fort Canning Road one way 2" kind=primary-link-1
              from n1363263434
              to -2112.6,888.2
              to n1363263485
            road "primary_link one way 4" kind=primary-link-1
              from n1363263538
              to -2196.2,807.0
              to -2206.4,813.8
              to -2219.0,819.9
              to n1363263232
            road "Central Expressway one way 4" kind=motorway-link-2
              at -2213.2,39.7
              to -2191.8,134.8
              to n74454947 bridge
            road "Circular Road" kind=residential-2
              from n1398048318
              to -1390.7,64.7
              to n246754375
            road "Queen Street one way" kind=tertiary-2
              from n1408671492
              to -1475.5,979.9
              to -1457.1,1022.2
              to n235180256 kind=tertiary-3
              to n410055846 kind=tertiary-1
            road "Republic Boulevard one way 2" kind=trunk-link-1
              from n1538307112
              to -234.0,1471.5
              to n1538307290
            road "Republic Boulevard one way 3" kind=trunk-link-2
              from n1538307199
              to -347.7,1290.1
              to -339.8,1290.1
              to -327.5,1297.4
              to -282.0,1372.0
              to n1538307112
            road "motorway_link one way 7" kind=motorway-link-2
              at 1565.0,1796.8
              to 1470.8,1758.9
              to 1453.9,1750.6
              to n2521023068
            road "Marina Coastal Expressway one way" kind=motorway-5
              from n1145506404
              to -829.8,-1412.2 bridge
              to -779.8,-1378.9
              to -729.7,-1354.1
              to -708.7,-1346.9
              to -662.9,-1336.0
              to -629.1,-1332.6
              to -602.0,-1332.2
              to -548.4,-1335.6
              to -500.2,-1345.1
              to n1539835307
              to -63.3,-1474.0 kind=motorway-6 ground
              to -7.5,-1486.2
              to n5006326691
              to 62.8,-1493.7 kind=motorway-5
              to 114.1,-1491.0
              to 152.5,-1484.6
              to 215.5,-1465.3
              to 236.6,-1456.4
              to 277.7,-1433.6
            road "Marina Gardens Drive one way" kind=secondary-3
              from n1539883549
              to 348.3,-880.0
              to n1629337146 kind=secondary-4
              to 504.0,-364.6 kind=secondary-3
              to 521.9,-323.6
              to 543.2,-286.1
              to 573.0,-247.3
              to 590.3,-228.0
              to 628.4,-193.2 kind=secondary-4
              to n3725683740 kind=secondary-3
              to n3772851631
              to 694.4,-154.1
              to 736.2,-137.2 kind=secondary-2
              to 777.9,-124.4
              to 806.2,-118.6 kind=secondary-3
              to 857.5,-113.4
              to 900.7,-114.3
              to 950.5,-120.5
              to 981.5,-125.1 kind=secondary-2
              to 1028.1,-135.1
              to 1085.0,-150.7
              to 1143.5,-171.3
              to n2596273195
            road "Banda Street one way" kind=residential-1
              from n246800772
              to n246800789
            road "primary_link one way 5" kind=primary-link-2
              from n1629037383
              to -1706.1,-173.2
              to n1629037351
            road "Teck Lim Road" kind=residential-2
              from n246800493
              to n243668563
            road "Marina Gardens Drive one way 2" kind=secondary-2
              from n2596273193
              to 1134.8,-181.9
              to 1092.4,-166.3
              to 1019.4,-147.1
              to 946.2,-134.2
              to 880.8,-131.7
              to 839.4,-131.4 kind=secondary-3
              to 817.5,-133.4
              to 763.3,-142.0
              to 726.9,-154.3 kind=secondary-4
              to 704.0,-164.5
              to n1985259425
              to n3772852905
              to 629.6,-212.8 kind=secondary-3
              to 605.6,-235.1
              to 557.0,-295.9
              to 529.3,-351.0
              to 520.4,-371.2
              to n1629337142
              to n1539883480
            road "Marina South Drive one way" kind=unclassified-1
              from n1539883507
              to n2519916627
              to n1539883536
            road "Sheares Avenue one way" kind=primary-5
              from n1145435573
              to 27.6,255.0 bridge
              to 46.6,180.2 kind=primary-4
              to 74.9,34.0 ground
              to 78.0,-8.5
              to 79.3,-57.9
              to 77.9,-108.8
              to 75.7,-168.3 kind=primary-5
              to n4600842971
              to 50.5,-314.5 kind=primary-4
              to 29.9,-400.4
              to 10.2,-471.9
              to n1539944100
              to n1539944119
            road "Kallang Paya Lebar Expressway one way" kind=motorway-link-1
              from n25455287
              to 1069.3,1671.3
              to 1079.7,1677.0
              to 1197.1,1724.2
              to 1214.7,1734.6
              to 1238.0,1753.9
            road "trunk_link one way 2" kind=trunk-link-1
              from n1683121661
              to -853.8,873.9
              to -832.5,881.5
              to -815.2,894.6
              to n1683121556
            road "Cantonment Road one way 3" kind=primary-3
              from n4745106067
              to -1857.2,-1505.2
              to -1952.2,-1311.5
              to -1968.4,-1277.8 kind=primary-4
              to n1829822457
              to n96443466 kind=primary-3
              to -2032.6,-1201.0
              to -2064.8,-1169.2 kind=primary-4
              to n237279064 kind=primary-5
              to n237348779
            road "Republic Boulevard one way 4" kind=trunk-link-2
              from n1783639818
              to -207.7,1460.7
              to -211.0,1460.8
              to n1538307248
            road "primary_link one way 6" kind=primary-link-2
              from n1790124158
              to -2168.5,134.8 bridge
              to -2168.3,125.6 ground
              to -2165.0,114.1
              to n1790124129
            road "Tank Road one way 2" kind=residential-1
              from n1363263536
              to -2174.7,720.4
              to n1790366566
              to n1363263413
            road "primary_link one way 7" kind=primary-link-1
              from n1816580034
              to -1607.0,99.2
              to n237170369
            road "River Valley Road one way" kind=primary-3
              from n237170319
              to n232207994
              to n1816580050
              to -1590.1,289.9
              to -1633.2,295.4
              to n246800552
              to n571492447
              to -2019.9,428.0
              to n74442029 kind=primary-4
            road "Gardens by the Bay one way" kind=unclassified-1
              from n1829374136
              to 645.3,-107.6
              to 637.9,-106.7
              to n1829374168
            road "Spottiswoode Park Road one way" kind=residential-1
              from n1829622670
              to -2357.2,-1750.3
              to -2348.1,-1754.5 kind=residential-2
              to -2338.1,-1756.2
              to n246801940
            road "Neil Road one way" kind=primary-link-1
              from n1829822435
              to -2341.5,-1405.8
              to -2353.9,-1406.5
              to n6703954131
              to n5138586950
            road "primary_link one way 8" kind=primary-link-1
              from n237279064
              to -2113.7,-1126.9
              to -2121.8,-1125.7
              to n1829822453
            road "primary_link one way 9" kind=primary-link-1
              from n1829822457
              to -1992.8,-1254.3
              to -2013.0,-1250.3
              to n1829822444
            road "Cantonment Road one way 4" kind=primary-link-3
              from n4600816516
              to -1668.6,-1729.7
              to n237278991
              to n388569725
            road "trunk_link one way 3" kind=trunk-link-2
              from n1829822434
              to -1913.4,-1822.7
              to -1907.4,-1817.4
              to -1895.6,-1801.8
              to n246801933
            road "primary_link one way 10" kind=primary-link-2
              from n1829822450
              to -2157.3,-1012.5
              to -2146.1,-1013.6
              to -2132.3,-1012.3
              to -2126.4,-1009.8
              to n1829822462
            road "primary_link one way 11" kind=primary-link-1
              from n1829822445
              to -2153.5,-1091.6
              to n1829822456
            road "trunk_link one way 4" kind=trunk-link-1
              from n1831961127
              to -1495.2,-1765.2
              to -1496.8,-1757.2
              to -1499.6,-1749.8
              to -1506.3,-1740.1
              to -1517.9,-1735.7
              to n1831961135
            road "Club Street" kind=residential-2
              from n1837448830
              to -1524.3,-655.4
              to -1457.9,-692.1
            road "Gemmill Lane one way" kind=residential-2
              from n1837448960
              to -1404.1,-560.7
              to -1393.2,-564.9
              to -1381.7,-565.4
              to -1376.1,-541.2 kind=residential-1
              to n1947625968
            road "Mohamed Ali Lane" kind=residential-1
              from n1831929820
              to -1511.3,-559.0
              to n5233472026
            road "Cantonment Link one way 2" kind=residential-2
              from n1318603823
              to -1790.1,-1646.7
              to -1776.6,-1637.6
              to -1763.9,-1635.5
              to n1829822448
            road "Outram Road one way 2" kind=primary-4
              from n7852537777
              to -2345.1,-897.2
              to n9559521797
              to -2267.0,-928.0
              to -2222.6,-954.4
              to -2206.3,-967.1
              to -2181.4,-991.7 kind=primary-6
              to n1829822450
              to -2141.7,-1049.7 kind=primary-5
              to n237218779
            road "Shenton Way one way 2" kind=trunk-5
              from n5282930784
              to n237154299
              to n4602135016 kind=trunk-4
              to -1270.8,-1612.8
              to n237154323
              to n246806211
            road "Collyer Quay one way" kind=trunk-3
              from n1633162341
              to n237315539
            road "Collyer Quay one way 2" kind=trunk-5
              from n237315539
              to -918.5,-387.6
              to -902.5,-374.8
              to -884.5,-354.4
              to -876.9,-339.8
              to -869.8,-323.6 kind=trunk-4
              to -865.4,-307.8
              to -862.4,-283.9
              to -860.8,-240.7
              to n1149139921
            road "Republic Boulevard one way 5" kind=primary-2
              from n1783639818
              to n235071053
            road "Havelock Road one way" kind=primary-6
              from n237296972
              to n1951506336
              to n597413159
              to n237219180
            road "Havelock Road one way 2" kind=primary-link-2
              from n243208171
              to -1946.2,-133.3
              to -1932.2,-134.5
              to n237296972
            road "New Bridge Road one way 2" kind=primary-link-1
              from n1852241862
              to -1960.4,-703.3
              to -1964.5,-703.9
              to -1971.6,-702.9
              to n1852241866
            road "Loke Yew Street" kind=residential-2
              from n1857823513
              to -1538.7,740.9
              to n4311466610
            road "North Bridge Road one way" kind=secondary-4
              at -908.4,1644.7
              to n3022208069
              to n6246027973
              to n232119579
              to -1008.5,1397.2
              to n4602578377
              to n232119616
              to n440009225
              to n233602622
              to n232119697
              to n232119730
              to -1195.6,893.8 kind=secondary-5
              to -1210.2,844.2
              to n232147312
              to -1268.1,660.6
              to -1272.2,641.7 kind=secondary-4
              to -1284.1,593.2 kind=secondary-5
              to -1287.5,567.3
              to n232147335
              to n232147366 kind=secondary-4
              to -1363.4,288.3
              to n4686999421
            road "Tanjong Pagar Road one way" kind=secondary-2
              from n237153849
              to -1557.4,-1677.3
              to -1568.2,-1606.8
              to -1575.3,-1585.3
              to n4745106075
              to -1594.5,-1553.6
              to n4745106074
              to n4745106078
              to -1633.3,-1254.1
              to n4745106068 kind=secondary-1
              to -1646.0,-1168.1
              to -1648.2,-1141.4
              to -1644.9,-1068.9
              to -1648.1,-1049.3
              to n237105802
              to n4600790494 kind=secondary-2
              to -1677.4,-928.9
              to n237089753
              to -1676.1,-862.0
              to -1670.8,-860.6
              to n4600790493
              to n1629037202
              to -1668.5,-935.1
              to n4982867037
              to n4982867047
              to -1640.0,-1065.7 kind=secondary-1
              to -1643.4,-1143.2
              to -1640.5,-1167.2
              to n4982867053
              to n4745192782 kind=secondary-2
              to n4745192776
              to n237153674
              to -1584.6,-1544.8
              to n4982866593
              to -1566.2,-1578.1
              to -1557.5,-1602.0
              to -1553.2,-1623.3
              to -1548.0,-1694.0
              to n4982866591
            road "New Market Road one way 2" kind=residential-1
              from n1951506336
              to -1851.9,-159.9
              to -1850.3,-168.9
              to -1860.3,-208.7
            road "primary_link one way 12" kind=primary-link-1
              from n237296721
              to -2204.0,-149.9
              to -2214.2,-155.1
              to -2222.3,-161.7
              to n249692339
            road "Solomon Street one way" kind=residential-1
              from n3756989295
              to n4723344715
              to -2059.7,-18.6
              to -2061.1,-13.3
              to -2059.5,-8.1
              to -2052.7,-3.8
              to -2047.3,-4.6
              to -2043.1,-8.1
              to -2041.6,-13.4
              to n5233639475
              to n3756989295
            road "Marina Coastal Drive one way" kind=unclassified-3
              from n1777643013
              to n6528381380
            road "Marina Coastal Drive one way 2" kind=unclassified-1
              from n6528381380
              to 432.8,-1743.9
              to 479.9,-1790.8 bridge
              to n13770297447
            road "Marina Coastal Drive one way 3" kind=unclassified-1
              from n13770297447
              to 526.6,-1828.4 bridge
              to 528.3,-1833.8
              to 526.6,-1839.1
              to 491.6,-1884.9
              to 486.9,-1888.0
              to 481.2,-1887.3
              to n13770297452
            road "Republic Avenue one way" kind=trunk-link-1
              at -302.7,1795.2
              to n2015359296
            road "motorway_link one way 8" kind=motorway-link-3
              from n2082809510
              to 550.3,-1134.1
              to n2521115163
            road "Marina Coastal Drive one way 4" kind=unclassified-2
              from n6528381379
              to 420.9,-1752.8
              to 416.9,-1763.4
              to 406.0,-1779.9
              to 391.5,-1795.7
              to 379.2,-1803.1
              to n6528381381
            road "Orchard Road one way 2" kind=primary-5
              from n571883509
              to n408999675
              to n408999670 kind=primary-4
              to -1955.0,1168.4 kind=primary-5
              to -1916.6,1171.8
              to n233544132 kind=primary-7
              to n7043034842 kind=primary-5
            road "motorway_link one way 9" kind=motorway-link-1
              at 1331.5,1707.9
              to 1322.3,1651.7
              to 1311.8,1618.0
              to 1299.4,1593.0
              to 1277.5,1561.8
              to 1251.4,1535.8
              to 1241.9,1527.6
              to 1222.2,1516.4
              to 1200.4,1509.1 kind=motorway-link-2
              to 1187.9,1507.1
              to 1165.6,1509.5
              to 1156.7,1512.0
              to 1146.4,1515.7
              to 1136.6,1521.8
              to 1115.9,1540.7 kind=motorway-link-3
              to 1074.7,1585.0
              to 1064.6,1593.0
              to 1042.6,1603.0
              to 1018.2,1607.9
              to 1005.2,1608.3
              to 963.9,1602.4
              to 927.0,1594.0
              to n25451929
            road "Marina Boulevard one way" kind=motorway-link-1
              from n2521115176
              to 726.9,-890.4
              to 752.6,-882.5
              to n2521115175
            road "motorway_link one way 10" kind=motorway-link-2
              from n2082809510
              to 542.8,-1129.2
              to 545.6,-1123.4
              to 546.6,-1111.7
              to 544.4,-1102.3
              to 540.0,-1093.9
              to 524.5,-1078.4
              to n8133473091
            road "Marina South Drive one way 2" kind=motorway-link-2
              from n5187405194
              to 933.3,-743.5
              to 913.7,-763.9
              to n6138767692
            road "Marina Link one way" kind=primary-3
              from n2521115163
              to 644.0,-1025.4
              to n2082806498 kind=primary-4
            road "Marina Link one way 2" kind=primary-5
              from n2521884270
              to n2521884269
              to n5006326688 kind=motorway-link-2
              to 583.7,-1144.8
              to 538.4,-1224.3
              to 500.0,-1275.1
              to 425.8,-1362.5
            road "Marina Costal Expressway one way" kind=motorway-link-2
              from n2082806498
              to 740.8,-899.1
              to 761.6,-878.1
              to n2521115175
            road "Marina Gardens Drive" kind=secondary-2
              from n2596273193
              to 1148.6,-184.9
              to 1148.7,-181.4
              to 1152.0,-175.2
              to n2596273195
              to 1163.5,-172.4
              to n1539883507
              to 1171.4,-185.2
              to 1170.4,-188.5
              to n1539883536
              to 1158.5,-194.7
              to n2596273193
            road "Central Boulevard one way" kind=primary-4
              from n2521884269
              to n2521115163
              to 547.9,-1072.3 kind=primary-3
              to n8133473091
              to n1539883480 kind=primary-5
              to n1539883549
              to n1539944119
              to n1539897210
              to n1539897224
              to n1539897226
              to -407.9,-786.5
              to n1629524122
              to n248420531
              to -713.3,-693.9
              to n2620921746 kind=primary-4
              to -822.1,-664.5
              to -840.9,-658.4 kind=primary-5
              to -880.0,-641.1
              to -926.0,-617.2
              to n248527331
            road "Commerce Street" kind=tertiary-2
              from n240002104
              to n2598074293
              to n1539888324
              to n1539944086
              to n1539944050
            road "Marina Coastal Drive one way 5" kind=tertiary-2
              from n2521884270
              to n4600842962
              to 817.7,-935.4 kind=tertiary-3
              to 823.7,-942.8
              to 828.2,-950.3
              to 830.3,-962.6
              to 829.0,-971.8
              to 823.4,-984.7
              to 779.9,-1042.4
              to 553.5,-1332.4
              to 529.1,-1357.1
              to 502.9,-1379.0
              to 413.8,-1441.5
              to 396.8,-1457.0
              to 345.8,-1519.2
              to 318.1,-1549.5
              to n2879240162 kind=tertiary-4
              to n1904310903 kind=tertiary-3
            road "Shenton Lane" kind=tertiary-2
              from n2598074292
              to n2598074293
            road "motorway_link one way 11" kind=motorway-link-3
              from n2521023068
              to 1419.6,1726.4
              to 1398.8,1710.5
              to 1357.1,1667.4
              to 1333.9,1622.9
              to 1326.4,1601.3
              to 1316.8,1549.1
              to 1314.0,1512.4
              to 1313.9,1458.4
            road "Marina Coastal Drive" kind=tertiary-2
              from n5006326670
              to 233.2,-1643.2
              to n4602213166
            road "Spring Street one way" kind=residential-1
              from n5809892935
              to -1686.0,-821.7
              to n246800772
            road "Marina Boulevard one way 2" kind=primary-4
              from n2521115176
              to n2082806498
              to n2521884270
            road "motorway_link one way 12" kind=motorway-link-2
              from n2521115175
              to 806.0,-820.2
              to 821.7,-795.2
              to 840.9,-755.3
              to 851.0,-739.2
              to 900.6,-675.1
              to 944.7,-623.9
            road "primary_link one way 13" kind=primary-link-1
              from n2750379975
              to -1655.6,-9.4
              to -1659.5,-8.7
              to -1663.0,-5.3
              to n2750379978
            road "unclassified one way" kind=unclassified-1
              from n2879240162
              to n2879240165
              to n2879240169
            road "tertiary_link one way" kind=tertiary-link-1
              from n2879240169
              to 272.6,-1572.0
              to 276.6,-1569.9
              to n2879240173
            road "unclassified one way 2" kind=unclassified-1
              from n2879240177
              to 266.3,-1584.2
              to n2879240169
            road "motorway_link one way 13" kind=motorway-link-1
              from n1539883493
              to 649.0,-1103.5
              to 633.3,-1103.3
              to 621.2,-1105.8
              to n5006326688
            road "Chin Chew Street" kind=secondary-4
              from n3059542035
              to n5600204998
            road "Hokien Street one way" kind=secondary-2
              from n5600204988
              to n3059534130
            road "Telegraph Street one way" kind=unclassified-2
              from n246817239
              to -963.3,-509.6
              to n1931861630 kind=unclassified-1
            road "Kirk Terrace one way" kind=residential-1
              from n3570841446
              to -1700.7,1327.6
              to -1706.3,1333.3
              to n3570841447
              to -1704.7,1341.9
              to n3570841454
            road "motorway_link one way 14" kind=motorway-link-2
              from n1363487255
              to -854.3,-1474.8 bridge
              to -868.0,-1490.7
              to -908.4,-1534.5
              to -936.9,-1562.7
              to -957.8,-1581.1
              to -976.1,-1594.2
              to -1008.9,-1613.7
              to -1048.7,-1628.6
              to -1092.2,-1639.5
              to -1124.4,-1644.6
              to -1182.8,-1650.8
              to -1227.2,-1654.0 kind=motorway-link-3 ground
              to n246806211
            road "Tan Tye Place one way" kind=residential-1
              from n612392130
              to -1870.1,383.5
              to -1867.3,378.3
              to n246800552
            road "Tras Street" kind=residential-2
              from n472730475
              to n554037804
              to n6691407338
            road "Temple Street one way" kind=residential-1
              from n246800441
              to -1776.4,-593.1
              to -1640.7,-623.9
              to n602549545 kind=residential-2
            road "Solomon Street one way 2" kind=residential-1
              from n5233639475
              to -2080.5,-95.7
              to -2078.8,-101.5
              to n3756989284
            road "Coleman Street one way 2" kind=tertiary-3
              from n232353783
              to -1205.4,534.4
              to -1210.4,537.5
              to n232147335
              to -1364.3,571.8
              to n232207249 kind=tertiary-4
            road "Sheares Avenue one way 2" kind=secondary-4
              from n8869838485
              to -247.4,-1256.1
              to n9913888164
              to n1633135165
              to -186.2,-1034.3
              to n1539897210
              to -96.7,-742.3 kind=primary-4
              to n1145466122 kind=primary-5
              to -12.0,-463.3 kind=primary-4
              to 7.0,-395.4
              to 28.2,-311.1
              to n4600842970
              to n4600842969
              to 55.0,-145.2
              to 58.2,-9.2
              to 55.3,32.7
              to 31.5,176.3
              to n6309963192 kind=primary-5 bridge
            road "Mohamed Sultan Road one way" kind=residential-1
              from n612392123
              to n74392496
              to -2311.2,522.0 kind=residential-2
              to n6232047028
              to n6232047026
            road "motorway_link one way 15" kind=motorway-link-2
              at 1239.2,1744.3
              to 1260.9,1766.0
              to 1281.2,1782.3
              to 1310.1,1799.5
            road "South Bridge Road one way 2" kind=secondary-2
              from n246800771
              to -1639.0,-763.1
              to -1648.3,-788.6
              to n5809892935
              to n4600790084
            road "Connaught Drive one way" kind=residential-1
              from n4451336954
              to -1041.1,248.4
              to n4451336941
            road "Connaught Drive" kind=residential-2
              from n3010901912
              to -1080.6,259.5
              to n9370421301 kind=residential-1
            road "Cantonment Road one way 5" kind=primary-2
              from n4600816516
              to -1647.3,-1731.0
              to n237278876
            road "Raffles Quay one way" kind=trunk-6
              from n237054972
              to n1931861630
              to n248527331
              to n240002104
            road "Pearl Bank" kind=unclassified-2
              from n249798852
              to -2203.7,-765.5
              to -2225.9,-778.5
              to -2254.1,-788.2
              to n5187147003
            road "Spottiswoode Park Road one way 2" kind=residential-1
              from n8061280812
              to -2333.0,-1427.2
              to n246801944
            road "Bukit Pasoh Road one way 2" kind=residential-1
              from n243668569
              to n4598518199
              to -2020.1,-1069.9
              to -2023.5,-1071.5
              to n4598518203
            road "Bukit Pasoh Road one way 3" kind=residential-1
              from n4598518203
              to n243668557
              to -2030.4,-1060.9
              to n243668558
            road "Bukit Pasoh Road one way 4" kind=residential-1
              from n243668569
              to n4598518203
            road "Keong Saik Road one way 2" kind=residential-1
              from n237170655
              to -1896.5,-688.8
              to -1910.5,-751.1
              to n1629037271 kind=residential-2
            road "Maxwell Road one way" kind=secondary-2
              from n4600790493
              to -1640.9,-874.3
              to n7705372941
              to n4745272384
              to -1563.8,-925.5
              to n237089858
              to n4982866573
              to n7012974711
              to n4745441756
              to n4745272381
              to -1554.9,-944.2
              to n4745272383 kind=secondary-3
              to n7705372942
              to -1645.3,-883.1
              to n1629037202
              to n237089753
              to n4600790084 kind=secondary-2
            road "Ann Siang Road" kind=residential-2
              from n1318603856
              to n1318603861
            road "Wallich Street" kind=residential-3
              from n241595238
              to n7097470158
            road "primary_link one way 14" kind=primary-link-1
              from n4600842961
              to -2136.7,486.6
              to -2129.1,489.4
              to -2121.7,494.3
              to -2110.0,505.4
              to n612392154
            road "motorway_link one way 16" kind=motorway-link-1
              from n2521884271
              to 788.4,-889.8
              to 785.3,-898.1
              to 785.8,-907.1
              to 787.7,-911.3
              to 790.9,-917.3
              to n4600842962
            road "Wallich Street 2" kind=residential-3
              from n241595238
              to -1389.6,-1121.7
              to -1395.4,-1128.0
              to -1413.7,-1140.9
              to n4600847758 kind=residential-2
            road "primary_link one way 15" kind=primary-link-1
              from n4600854642
              to -256.2,818.8
              to -247.4,827.8
              to -236.6,843.3
              to n4600854637
            road "motorway_link one way 17" kind=motorway-link-1
              from n4602135016
              to -1245.9,-1595.9
              to -1237.8,-1606.2
              to -1234.0,-1609.4
              to -1224.6,-1613.1
              to n4602135005
            road "Marina Coastal Drive one way 6" kind=unclassified-2
              from n1904310903
              to 280.5,-1606.9
              to 280.2,-1613.7
              to n1777643013
            road "Marina Coastal Drive one way 7" kind=unclassified-1
              from n4602213163
              to n1777643013
            road "Fort Road South" kind=unclassified-3
              from n7153058636
              to n7153058639
            road "primary_link one way 16" kind=primary-link-1
              from n4606287878
              to -2202.4,-71.2
              to n4606287874
            road "Chin Swee Road one way 2" kind=primary-link-1
              from n1790098230
              to -2251.2,-179.0
              to -2246.9,-179.1
              to -2240.2,-184.4
              to n4606287879
            road "tertiary_link one way 2" kind=tertiary-link-1
              from n4616343182
              to -1245.2,-153.5
              to -1246.2,-147.0
              to -1244.0,-140.9
              to -1239.1,-136.5
            road "Buyong Road one way 2" kind=tertiary-3
              at -2392.0,1203.4
              to -2390.6,1190.5
              to n571883509
            road "Handy Road one way" kind=residential-2
              from n236429505
              to n233544132
              to -1901.1,1218.4
              to -1906.3,1224.9
              to -1913.4,1230.3
              to -1922.6,1233.4
              to -1984.3,1236.5
              to -2007.4,1240.4
              to -2029.4,1248.8
              to n1675287301
              to n4684627247
              to -2086.6,1284.8 kind=residential-3
              to -2107.9,1292.6
            road "Oldham Lane one way" kind=residential-1
              from n1675287301
              to -2060.1,1261.3
              to -2063.5,1258.7
              to n4682910150
            road "Marina East Drive one way" kind=primary-2
              at 2312.6,1740.1
              to 2283.7,1699.2
              to n7153058636
              to 2277.6,1705.2 kind=primary-3
              to 2301.9,1762.9
              to 2305.5,1784.4
            road "Anson Road one way 2" kind=residential-2
              from n4684502273
              to n6897440292
            road "North Bridge Road one way 2" kind=secondary-2
              from n4686999421
              to -1368.7,257.9
              to -1371.4,247.9
              to n4686999423 bridge
            road "South Bridge Road one way 3" kind=secondary-2
              from n4686999423
              to n4686999424
            road "South Bridge Road one way 4" kind=secondary-4
              from n4686999424
              to n237012067
              to n1398048399
              to n7904178876
              to n237012083
              to n237218629
              to n237012110
              to n237218667
              to n237012130
              to n246806298
              to -1545.7,-397.6
              to n237012179
              to n26781973
              to n1837448982
              to n602549545
              to n249690363
              to n26782131
              to n246800771
            road "primary_link one way 17" kind=primary-link-1
              from n5765341406
              to -1611.2,1200.1
              to -1614.8,1171.4
              to n4688852440
            road "Spottiswoode Park Road one way 3" kind=residential-1
              from n4710801492
              to -1902.4,-1761.9
              to -1913.6,-1753.1
              to -1928.0,-1746.5
              to -1941.8,-1744.2
              to -1960.8,-1748.0
              to n5138586955
              to n4710801500
              to -2109.6,-1799.2
              to -2145.7,-1825.3
              to -2159.1,-1829.6
              to n4710801499
              to -2160.8,-1824.1
              to -2144.8,-1818.7
              to -2106.3,-1790.4
              to n246801932
              to n5138586956
              to -2004.8,-1754.7
              to -1948.4,-1738.6
              to -1931.1,-1740.3
              to -1895.0,-1755.1
              to -1885.9,-1754.1
              to n237279254
            road "Keppel Road one way 2" kind=trunk-link-1
              from n4710862646
              to -2187.6,-1943.7
              to -2194.7,-1937.4
              to -2197.3,-1930.9
              to -2195.9,-1921.7
              to n4710862651
            road "Everton Road 2" kind=residential-2
              from n4710862656
              to -2080.1,-1592.6
              to -2088.1,-1576.1
              to -2127.6,-1514.8
              to n1829436720
            road "East Coast Parkway one way" kind=motorway-4
              at 1495.7,1787.5
              to n6064305393
              to 1227.3,1699.3 kind=motorway-3
              to 926.7,1601.8
              to n25451929
              to 748.2,1539.4 kind=motorway-5
              to 715.0,1522.7
              to 529.6,1425.2 bridge
              to 276.0,1296.5
              to n6749812859
              to 40.4,1175.0 kind=motorway-4
              to 8.8,1156.2
              to -33.3,1125.7
              to -56.7,1103.5
              to -87.5,1062.7
              to -113.2,1017.3
              to -121.5,997.3
              to -131.2,954.2
              to -131.0,899.1
              to -128.9,873.4
              to -121.4,832.2
              to -82.3,669.0
              to -68.6,620.7
              to -49.6,539.6
              to n1145435573
            road "Marina Grove 2" kind=unclassified-2
              from n1637003311
              to n11465244435
              to n11465244446
            road "Marina Coastal Drive one way 8" kind=tertiary-1
              from n1904310903
              to n4602213163
            road "North Bridge Road one way 3" kind=secondary-2
              from n4686999421
              to n7094958410
              to -1380.6,250.3
              to n323137286 bridge
            road "Republic Avenue one way 2" kind=primary-3
              at -168.1,1794.7
              to -193.5,1761.4
              to -201.6,1734.6
              to -208.7,1704.5 kind=primary-2
              to -211.8,1681.6
              to -208.5,1639.5
              to -202.9,1611.5
              to -198.7,1571.7
              to -189.2,1505.5 kind=primary-3
              to n235069384 kind=primary-2
            road "residential 2" kind=residential-2
              from n4724301487
              to n4724301488
            road "residential 3" kind=residential-2
              from n4724301589
              to n4724301590
            road "residential 4" kind=residential-2
              from n4724301591
              to n4724301592
            road "residential 5" kind=residential-2
              from n4724301593
              to n4724301594
            road "Fullerton Road one way 2" kind=trunk-5
              from n1149139921
              to -854.9,-168.3
              to n1149139906 kind=trunk-6
              to -840.5,-74.1 kind=trunk-4
              to n13812791644 kind=trunk-5
              to -832.3,0.9
              to -830.9,33.4 kind=trunk-4
              to n237054768
              to n237012287
            road "Battery Road one way" kind=tertiary-2
              from n249766692
              to -946.9,-78.9
              to -1020.1,-147.8
              to -1025.8,-151.2
              to n249766690 kind=tertiary-3
            road "Marina View one way" kind=unclassified-1
              from n4724570673
              to -707.9,-496.9
              to -737.7,-593.7
              to -741.3,-599.2
              to -745.9,-602.8
              to n4724570677
            road "Collyer Quay one way 3" kind=trunk-5
              from n4724612488
              to -841.0,-287.5
              to -842.3,-301.7 kind=trunk-6
              to -854.2,-342.9
              to -864.2,-375.5 kind=trunk-5
              to n237054972 kind=trunk-6
            road "Marina View one way 2" kind=tertiary-2
              from n9913888162
              to n1539897215
              to -830.4,-920.2
              to n1539888324
              to n2620921746 kind=tertiary-3
              to n248536068
              to -730.7,-596.1 kind=tertiary-2
              to -700.7,-497.8
              to -694.0,-491.9
              to n5282854973
            road "Maxwell Road one way 2" kind=secondary-3
              from n4724923624
              to -1177.4,-1187.7
              to -1193.0,-1182.1 kind=secondary-4
              to n4724923625
              to n4724923628 kind=secondary-3
              to n4729974845
              to -1295.5,-1123.2
              to n7097470164 kind=secondary-2
              to n4724923626
              to n5233472032
              to n241595239
              to -1345.6,-1081.8 kind=secondary-3
              to n1318569081 kind=secondary-4
              to n1318544659 kind=secondary-3
              to n237055115 kind=secondary-4
              to -1187.0,-1168.4 kind=secondary-3
              to -1169.1,-1175.5
              to n246800870 kind=secondary-4
              to n237055087
            road "primary_link one way 18" kind=primary-link-2
              from n6639134876
              to -214.5,848.5
              to -199.9,827.8
              to n4726227141
            road "primary_link one way 19" kind=primary-link-1
              from n3071267396
              to -1000.9,723.9
              to -1003.1,730.2
              to -1005.4,740.9
              to n4726412914
            road "trunk_link one way 5" kind=trunk-link-2
              from n4726775783
              to -827.4,620.7
              to -838.1,615.6
              to n4726775785
            road "tertiary_link one way 3" kind=tertiary-link-1
              from n4732339357
              to -242.0,903.3
              to n4732339359
            road "Victoria Street one way 2" kind=primary-3
              at -1045.8,1627.9
              to -1077.7,1553.4
              to -1122.8,1456.0
              to n4602578375 kind=primary-4
              to n206833715 kind=primary-3
              to n206833694
              to -1250.9,1150.9
              to n206833639
              to -1362.6,932.6
              to -1381.9,895.1
              to -1404.9,843.5
              to n233421104
            road "secondary_link one way" kind=secondary-link-1
              from n1398080906
              to -858.7,1188.4
              to n4732407688
            road "secondary_link one way 2" kind=secondary-link-1
              from n4602578383
              to -862.8,1255.6
              to n4732525709
            road "Clemenceau Avenue one way 3" kind=primary-link-1
              from n4734606540
              to -2126.1,680.7
              to n4734606543
            road "Kampong Bahru Road one way" kind=primary-3
              from n6703954131
              to n26782168
            road "River Valley Road one way 2" kind=primary-3
              from n5432103372
              to -1951.3,414.1
              to -1906.9,399.2
              to n612392130 kind=primary-4
              to -1640.7,310.6 kind=primary-3
              to -1603.1,302.6
              to -1583.3,300.2 kind=primary-4
              to n74442036
              to n4600842967
            road "Coleman Street" kind=tertiary-4
              from n232207249
              to n232207771
              to -1527.1,604.3 kind=residential-3
              to -1542.1,608.2
              to -1544.3,610.3
              to n7229619775 kind=residential-2
            road "Havelock Road one way 3" kind=primary-4
              at -2397.2,-55.4
              to -2369.1,-50.8
              to -2313.3,-61.8
              to n571492380 kind=primary-6
              to n237296882 kind=primary-5
              to n1790124104
              to n4606287874
              to n249693001 kind=primary-4
              to -2128.0,-104.3
              to n4723344719
              to n3756989284
              to -2033.3,-103.7
              to -2017.1,-105.1
              to -2006.7,-106.4
              to n4600694846
              to n4723328877
              to n237296972
            road "Upper Cross Street one way" kind=motorway-link-1
              from n1363294010
              to -2081.2,-149.0
              to -2151.7,-133.9
              to -2176.6,-127.0
              to -2187.3,-120.9
              to -2201.2,-106.1
              to -2206.4,-98.3
            road "Smith Street one way 2" kind=residential-1
              from n7027603394
              to n4744279230
              to n4739815319
            road "Cantonment Road one way 6" kind=primary-link-1
              from n4745106066
              to -1827.6,-1536.0
              to -1831.7,-1537.8
              to n4745106067
            road "Cantonment Road one way 7" kind=primary-3
              from n4745106066
              to -1788.9,-1586.8
              to n246806272
              to n237154569
            road "Duxton Hill 2" kind=residential-2
              from n4745151601
              to -1735.2,-996.8
              to -1786.8,-1052.2
              to -1794.1,-1049.4
              to -1817.8,-1085.2
            road "Tras Street 2" kind=residential-2
              from n6691407335
              to n237154096
            road "Neil Road one way 2" kind=secondary-3
              from n4600790084
              to -1711.3,-883.7
              to n8153290963
              to n6048295590
              to n26782339
              to -1817.6,-986.1
              to n26782248
              to n26782154
              to n7030292676
              to -1902.6,-1145.7
              to -1924.5,-1176.1
              to n3324524505
              to -1959.1,-1208.1
              to -1973.0,-1220.1 kind=secondary-4
              to n3324524499
              to n96443466
              to n1829822444 kind=secondary-3
              to -2135.6,-1306.9 kind=secondary-4
              to n243210137
              to n1829822435
            road "Neil Road one way 3" kind=secondary-3
              from n1829822435
              to -2322.3,-1409.3
              to n8061280812
              to n26782168
            road "Peck Seah Street one way" kind=residential-1
              from n4982866573
              to n4745441756
              to n4745441753
              to n4745272381 kind=residential-2
              to n237089858
            road "Peck Seah Street" kind=residential-3
              from n4745441753
              to -1519.5,-997.9
              to n4600847758 kind=residential-2
              to n237105259
              to n4982866863
              to n237105451
            road "trunk_link one way 6" kind=trunk-link-1
              from n1538307293
              to n235069384
            road "trunk_link one way 7" kind=trunk-link-1
              from n5243406658
              to n235071053
            road "motorway_link one way 18" kind=motorway-link-1
              from n1539835288
              to -497.1,-1331.7
              to -462.7,-1343.8
              to n1539835307
            road "Ann Siang Road 2" kind=residential-2
              from n1318603856
              to -1506.2,-811.4
              to n246806289
            road "Marina Coastal Drive one way 9" kind=tertiary-1
              from n5006326665
              to 197.4,-1698.6
              to 190.6,-1695.3
              to 188.1,-1691.9
              to 187.2,-1683.7
              to 191.8,-1676.8
              to n5006326670
              to n5006326669
              to 210.0,-1691.8
              to n5006326665
            road "Park Street one way" kind=secondary-2
              from n2598074292
              to n1539897215
              to n1539944025
              to n1539944070
              to n11389450855
              to n11389450853
              to n1633135165
            road "Marina South Drive one way 3" kind=unclassified-1
              from n2653101274
              to 981.1,-597.3
              to 982.4,-593.3
              to 986.7,-590.1
              to 992.1,-588.6
              to 1012.2,-590.4
              to 1020.6,-586.3
              to n4712021480
              to 1032.8,-581.4
              to 1014.5,-603.3 kind=unclassified-2
              to 1006.7,-609.6
              to 1002.3,-611.1
              to 996.5,-610.7
              to 988.9,-608.9
              to n2653101274
            road "Esplanade Drive one way" kind=trunk-4
              from n237012287
              to -833.8,105.3
              to -860.9,392.2 bridge
              to -868.2,489.8 ground
              to -869.1,534.6
              to n12071463880 kind=trunk-5
              to n233879753
            road "primary_link one way 20" kind=primary-link-1
              from n5165895535
              to -1846.6,-415.4
              to -1840.3,-414.4
              to n5165895530
            road "primary_link one way 21" kind=primary-link-1
              from n7016947288
              to -162.7,605.0
              to -180.6,616.4
              to n5165925965
            road "Teo Hong Road one way" kind=residential-1
              from n237170670
              to -2077.3,-943.3
              to n5172094036
            road "Second Hospital Avenue one way" kind=residential-1
              from n4736504887
              to -2294.9,-942.7
              to n249792083
              to -2308.3,-936.5
              to n5172094047
            road "Marina Coastal Expressway one way 2" kind=motorway-5
              at 231.5,-1484.0
              to 189.2,-1499.8
              to 145.4,-1512.1 kind=motorway-6
              to 96.2,-1520.0
              to 55.1,-1522.4
              to 6.0,-1518.3
              to -35.0,-1510.8
              to -426.9,-1392.1
              to n1539835341 bridge
              to -516.6,-1365.4 kind=motorway-5
              to -567.6,-1357.6
              to -623.7,-1357.4
              to -645.6,-1359.3
              to -694.0,-1368.8
              to -744.6,-1386.1
              to -767.8,-1397.6
              to -828.4,-1441.2
              to n1363487255
            road "Marina Link one way 3" kind=motorway-link-2
              from n5006326693
              to 201.3,-1454.7
              to 235.5,-1436.7
              to 260.8,-1422.9
              to 308.4,-1389.5
              to 363.3,-1337.7
              to 408.4,-1286.1
              to 451.5,-1231.9
              to n2521115159
            road "motorway_link one way 19" kind=motorway-link-2
              from n5006326691
              to 56.7,-1482.3
              to 107.0,-1478.0
              to n5006326693
            road "Republic Avenue one way 3" kind=primary-3
              from n1538307293
              to n1538307290
              to -210.8,1590.4 kind=primary-2
              to -218.0,1644.9
              to -220.5,1679.9
              to -218.8,1723.5 kind=primary-3
              to -208.5,1768.0
              to -191.0,1792.8
            road "Merchant Loop one way 2" kind=residential-1
              from n5186902946
              to -2011.4,137.0
              to n74444087
              to -2020.7,146.2
              to -2020.6,151.0
              to -2018.4,155.3
              to -2014.7,158.3
              to -2010.0,159.6
              to -2005.2,158.8
              to -2001.2,156.2
              to -1998.8,151.7
              to -1998.1,147.3
              to -1999.5,142.7
              to n5186902946
            road "primary_link one way 22" kind=primary-link-1
              from n5187250342
              to -1556.5,279.7 bridge
              to n1816580050 ground
            road "motorway_link one way 20" kind=motorway-link-3
              from n2521884271
              to n2521884270
            road "Marina Link one way 4" kind=motorway-link-2
              at 1015.1,-643.9
              to n5187405194
            road "New Bridge Road one way 3" kind=primary-4
              from n1852241862
              to n237154127
              to -2036.0,-888.7
              to n237170670 kind=primary-5
              to n3344055036 kind=primary-4
              to n237170711 kind=primary-5
              to n237348779
              to n1829822453 kind=primary-4
              to -2167.5,-1177.5
              to -2180.1,-1194.7
              to -2201.3,-1221.0
              to n4736508655
              to n6703954131
            road "Everton Road one way 2" kind=residential-1
              from n4710801500
              to n246801932
              to -2068.3,-1661.7
              to n4710862656
              to -2060.2,-1680.3
              to n5138586956
              to n5138586955
            road "Keppel Road one way 3" kind=trunk-3
              from n4710862646
              to n388569722
            road "Cantonment Link one way 3" kind=residential-2
              from n388569724
              to n246801934
              to n246801933
              to n4710801492
              to -1882.0,-1759.7
              to n237279254
              to -1813.9,-1676.9
              to n1318603823
            road "Craig Road" kind=residential-2
              from n4982867053
              to n4745106068
              to n246800809
              to n246800861
              to n7030292675
            road "Pearl Bank 2" kind=unclassified-2
              from n249798852
              to -2149.1,-720.2
              to -2146.8,-715.8
              to -2146.5,-712.0
              to -2149.0,-704.8
              to -2158.4,-693.6
            road "Merbau Road" kind=residential-2
              from n74407251
              to -2202.0,429.5
              to n471501777
            road "Prince Edward Road one way" kind=tertiary-3
              from n237154196
              to -1168.1,-1331.5
              to -1240.9,-1318.0
              to -1248.6,-1313.2
              to n237154438
            road "River Valley Road one way 3" kind=primary-link-1
              from n74442029
              to -2031.4,435.2
              to -2030.9,440.4
              to n5432103372
            road "Bukit Pasoh Road one way 5" kind=residential-1
              from n3344055036
              to n1836286636
            road "Rhu Cross one way" kind=residential-1
              from n5278629766
              to 266.9,1686.0
              to 264.4,1693.8
              to 259.6,1698.7
              to 248.8,1702.2
              to 245.7,1707.2
              to 245.4,1713.0
              to 247.9,1718.2
              to 252.7,1721.6
              to 260.1,1722.0
              to 266.2,1717.9
              to n247008452
              to 268.1,1698.2
              to 271.5,1687.3
              to n5278629766
            road "Gardens by the Bay one way 2" kind=unclassified-1
              from n3772852905
              to n3725683740
              to 652.4,-154.1
              to n1829374168 kind=unclassified-2
              to n1829374038
              to 606.7,-53.4
              to n1829374182
              to 639.8,-8.6
              to 650.6,-7.9
              to 680.6,-16.6
              to 702.3,-30.2
              to 712.9,-49.8
              to 714.1,-68.0
              to 712.2,-73.5
              to 705.3,-83.0
              to n1829374136
            road "Canning Rise" kind=residential-2
              from n7229619775
              to -1609.7,670.8
              to -1637.7,703.3
            road "trunk_link one way 8" kind=trunk-link-1
              from n1831960739
              to -1437.2,-1711.6
              to n1831960718
            road "Shenton Way one way 3" kind=trunk-link-1
              from n5282930784
              to -1235.9,-1526.2
              to -1241.7,-1531.8
              to -1250.6,-1535.7
              to n5282930777
            road "Middle Road one way" kind=secondary-3
              from n233783186
              to n233783297
              to -798.1,1174.8
              to n1398080906
              to -852.8,1197.2
              to n233801368
              to n232082528
              to -881.7,1236.4 kind=secondary-2
              to -889.3,1243.3
              to -907.0,1250.2
              to -935.8,1264.5
              to n232119616 kind=secondary-3
              to -1108.7,1332.3
              to n206833715 kind=secondary-4
              to n233432515 kind=secondary-3
              to n233801197
              to -1343.4,1431.3
              to n5243492222 kind=secondary-4
              to n233801222
              to -1435.5,1468.7 kind=secondary-3
            road "Sheares Link one way" kind=residential-1
              from n4600842971
              to 57.0,-220.3
              to 53.4,-222.7
              to n4600842970
              to -2.9,-210.5 kind=residential-2
              to -43.4,-199.1 kind=residential-3
              to n1832238034 kind=residential-2
              to n1832238040
            road "Merchant Road one way" kind=residential-2
              from n74460234
              to n1275666727
              to n1790124129
              to -2120.7,98.2
              to -2100.1,92.0 kind=residential-1
              to -2098.1,86.9
              to -2098.0,81.2
              to n237251504
              to -2109.4,76.2 kind=residential-2
              to -2171.9,96.0 kind=residential-3
              to n4600634120
              to n4600634119
            road "Esplanade Drive one way 2" kind=trunk-4
              from n233879421
              to n4726775785
              to -851.5,526.3
              to -850.7,492.7 kind=trunk-5
              to -846.4,439.0
              to -842.9,396.6 kind=trunk-4
              to -816.2,106.2 bridge
              to n11259104759 ground
              to n237054576
            road "motorway_link one way 21" kind=motorway-link-2
              from n4602135005
              to -1146.5,-1602.5
              to -1105.6,-1596.0 bridge
              to -1048.6,-1578.2
              to -1014.5,-1561.4
              to -997.1,-1550.5
              to -960.5,-1522.3
              to n1145506404 kind=motorway-link-1
            road "Beach Road one way" kind=secondary-4
              from n232353226
              to -1010.2,740.7
              to n4726412914
              to -1002.2,752.3 kind=secondary-5
              to -982.4,817.2
              to -965.7,881.5
              to n232082341
              to -959.5,931.0 kind=secondary-2
              to -944.5,978.7
              to n232082494 kind=secondary-3
              to n440009224 kind=secondary-4
              to n232082528
              to n4602578385 kind=secondary-3
              to n4732525709 kind=secondary-4
              to n233602664
              to -812.7,1365.4 kind=secondary-5
              to n6246027974 kind=secondary-4
              to n3022208072
              to -672.1,1616.8
              to n232082907
              to -620.1,1707.2 kind=secondary-3
            road "China Street one way" kind=secondary-2
              from n6552271865
              to n6408411362
            road "Central Expressway one way 5" kind=motorway-3
              at -2382.0,-347.8
              to -2314.2,-272.8
              to -2293.1,-245.8
            road "South Bridge Road one way 5" kind=secondary-4
              from n246800771
              to -1632.5,-756.9
              to -1646.7,-810.8
              to n4600790493
            road "Stanley Street" kind=residential-2
              from n5817288340
              to -1199.1,-978.8
            road "residential one way" kind=residential-1
              from n5817301660
              to n5817301661
            road "Bras Basah Road one way" kind=primary-5
              from n7043034842
              to n240686973
              to -1782.3,1214.0
              to -1763.7,1214.4
              to n233497138
              to n233497244
              to n4688852440 kind=primary-4
              to n4672154362
              to n235180256
              to n233432379
              to n206833639
              to n232119730
              to -1063.7,940.5 kind=primary-5
              to n232082341
              to n595254399 kind=primary-6
              to n1683121661
              to n172510077 kind=primary-5
              to n233497179
            road "Fort Canning Road" kind=secondary-3
              from n243413548
              to -1847.8,1056.1
              to -1889.3,1046.7
              to -1931.7,1033.6
              to -1993.6,1003.3
              to -2031.5,982.4
              to n1363263434
            road "Tan Tye Place" kind=residential-2
              from n246800552
              to -1899.2,275.0
              to -1910.5,254.0
              to n4672154348
            road "Kirk Terrace" kind=residential-2
              from n3570841447
              to -1729.7,1340.9
              to -1742.9,1346.2
              to -1749.4,1351.9
              to -1761.5,1368.7
            road "Republic Boulevard one way 6" kind=primary-2
              from n235069384
              to n5243406658
              to -170.0,1358.1
              to -138.9,1164.7
              to -135.2,1151.0
              to -120.6,1118.2
              to -47.0,1019.7
              to -34.6,998.7
              to -1.2,917.5
              to n233879721
            road "Republic Boulevard one way 7" kind=trunk-link-2
              from n235071053
              to -211.6,1474.1
              to n1538307248
            road "Blair Road" kind=residential-2
              from n246801944
              to -2356.6,-1515.2
              to -2365.2,-1523.7
              to -2370.6,-1532.1
            road "Havelock Road one way 4" kind=primary-4
              from n237296820
              to n243213817
              to -2322.1,-76.2
              to -2357.7,-68.6
              to -2380.5,-69.1
              to -2394.8,-73.1
            road "Chin Swee Road one way 3" kind=primary-2
              from n1790098230
              to -2256.8,-179.2
              to n249692307 kind=primary-3
              to n237296820
              to n237296882
            road "Upper Cross Street one way 2" kind=primary-4
              from n237012179
              to -1597.0,-449.7
              to n237170538 kind=primary-5
              to n237219117 kind=primary-4
              to -1941.8,-339.8
              to n237296520 kind=primary-5
              to -2008.8,-309.7 kind=primary-4
              to -2026.9,-296.4
              to -2037.5,-285.4
              to -2050.8,-265.9 kind=primary-5
              to n237296580
              to -2062.0,-233.0 kind=primary-4
              to -2063.4,-216.7
              to n237296663
              to n5233639468
            road "Marina Grove one way" kind=unclassified-2
              from n11465244446
              to 738.6,-419.8
              to 754.3,-422.0
              to 787.8,-422.6
              to 808.6,-427.0
              to n12615243863
              to 742.1,-428.6 kind=unclassified-1
              to 720.5,-422.5
              to n12615243874
              to n12615243873
              to 654.2,-389.3
              to 620.7,-377.9
              to 545.7,-358.0
              to 547.8,-351.7
              to 554.7,-346.2
              to 561.3,-359.3
              to 593.4,-368.4
              to 630.4,-373.5
              to n1637003311
            road "Palmer Road one way" kind=residential-2
              from n237154299
              to n5282930777
              to n554037746 kind=residential-3
              to -1300.4,-1522.2
              to -1324.2,-1506.0
              to -1353.9,-1491.7 kind=residential-5
              to n237154414
            road "Enggor Street one way" kind=tertiary-3
              from n237154414
              to -1388.5,-1485.7
              to -1437.9,-1504.0
              to -1456.8,-1513.5 kind=tertiary-2
              to n6228532829
              to n237154096 kind=tertiary-3
              to n4982866593
            road "Parliament Place one way" kind=tertiary-3
              from n232147366
              to -1290.0,337.5
              to n232278983 kind=tertiary-2
              to -1208.2,303.2
              to -1184.0,296.3
              to n6856578454 kind=tertiary-1
              to -1173.5,295.3
              to -1165.3,300.0
              to n232353925
            road "Temasek Avenue one way 2" kind=tertiary-2
              from n595497898
              to -483.1,1099.6
              to -490.4,1097.5
              to n1130779933
            road "Boon Tat Street one way" kind=residential-3
              from n26782293
              to n246801952
              to n246801953
              to n3059598143
              to n241595237
              to n555760573
              to n240002104
            road "residential 6" kind=residential-2
              from n5270952271
              to n243233999
            road "Market Street one way" kind=residential-2
              from n9559558762
              to n246810568
            road "Market Street one way 2" kind=residential-1
              from n246810673
              to -1090.3,-458.5
              to -1078.7,-458.3
              to -1067.7,-463.9
              to -1063.3,-469.1
              to -1043.5,-528.3 kind=residential-2
              to -1040.0,-542.2
              to -1038.8,-554.5
              to n249766698 kind=residential-1
            road "motorway_link one way 22" kind=motorway-link-2
              from n243494149
              to 833.0,1598.8
              to 841.4,1603.9
              to 1172.4,1730.5
              to 1193.8,1741.4
              to 1206.6,1750.1
              to 1227.4,1767.3
              to 1245.9,1787.8
            road "River Valley Road one way 4" kind=primary-2
              at -2375.4,605.0
              to n8167517153
              to -2324.8,553.3
              to n612392123
              to -2281.2,523.7
              to -2258.8,512.7
              to -2235.4,503.9
              to -2211.3,497.3
              to n682938046 kind=primary-3
              to n4600842961 kind=primary-4
              to n612392126
              to n1275666718
              to n5794881171 kind=primary-3
              to n5432103372
            road "Kim Yam Road one way" kind=residential-2
              at -2398.0,278.0
              to n682933110
            road "Straits Boulevard one way" kind=secondary-3
              from n237055087
              to -1018.3,-1220.8
              to n1539897211 kind=secondary-4
              to -912.1,-1254.2 kind=secondary-1
              to -838.7,-1274.3 kind=secondary-3
              to -756.7,-1290.6
              to -676.5,-1296.9
              to n1539835288
              to -563.7,-1306.1 kind=secondary-2
              to -495.8,-1323.4
              to -360.7,-1363.3
              to -336.6,-1367.2
              to -322.9,-1366.7
              to -303.1,-1361.0
              to n8869838485
            road "Upper Circular Road" kind=residential-2
              from n237012067
              to -1472.5,143.4
              to n237170343
            road "Ophir Road one way 2" kind=trunk-5
              at -679.8,1696.3
              to n232082907
              to n232083078 kind=trunk-4
              to -612.1,1644.7 kind=trunk-3
              to -575.8,1612.5
              to -521.9,1561.1 bridge
              to -463.0,1487.1
              to -445.5,1465.1 ground
              to -418.8,1419.3
              to -413.7,1402.2
              to -406.2,1363.5
              to -394.0,1336.1
              to n1538307199 kind=trunk-4
              to n12741088964
            road "Ayer Rajah Expressway one way" kind=motorway-4
              at -2225.1,-1936.5
              to -1886.2,-1825.5 bridge
              to -1391.8,-1667.0
              to -1331.5,-1651.0
              to -1276.5,-1639.5
              to -1104.4,-1614.0
              to -1044.4,-1595.0
              to -1008.9,-1577.0
              to -977.6,-1556.2
              to -961.0,-1541.7
              to n1145506404
            road "Ayer Rajah Expressway one way 2" kind=motorway-3
              from n1363487255
              to -943.1,-1548.2 bridge
              to -983.5,-1584.0 kind=motorway-4
              to -1016.9,-1603.5
              to -1052.1,-1618.2
              to -1093.9,-1628.8
              to -1122.2,-1633.3
              to -1274.4,-1652.2
              to -1302.5,-1657.0
              to -1356.8,-1670.1
              to -1511.3,-1718.1
              to -2224.3,-1950.8
            road "Seah Street" kind=residential-2
              from n232082494
              to -959.5,1098.7
              to -1002.5,1117.0
              to n232119697
            road "Marina Coastal Expressway one way 3" kind=motorway-link-3
              from n2598074281
              to -853.8,-1295.0
              to -876.8,-1281.0
              to -913.3,-1266.8
              to n4602143859
            road "Queen Street 2" kind=tertiary-5
              from n4602578386
              to n595267991
              to -1247.4,1493.1 kind=tertiary-4
              to -1235.3,1521.9 kind=tertiary-3
            road "Republic Boulevard one way 8" kind=trunk-link-2
              from n1538307112
              to -228.1,1472.9
              to -212.5,1485.8
              to n1538307293
            road "Spottiswoode Park Road" kind=residential-2
              from n4710801499
              to -2251.8,-1857.8
              to n4600811395
              to n1829622670
              to -2332.1,-1773.3
              to -2331.2,-1764.0
              to n246801940
              to -2290.6,-1702.3
              to -2251.9,-1641.0
              to -2248.8,-1633.1
              to -2247.4,-1625.2
              to -2248.2,-1616.6
              to -2251.4,-1608.5
              to n9987737918
              to n9987737920
              to n9987737922
              to -2289.2,-1549.2
              to -2295.2,-1537.3
              to -2298.1,-1529.7
              to n246801944
            road "Gardens by the Bay one way 3" kind=unclassified-2
              from n1829374136
              to 647.3,-113.9
              to 646.1,-118.9
              to 647.0,-123.2
              to n3772851631
              to n1985259425 kind=unclassified-1
            road "Tanjong Rhu Road one way" kind=tertiary-1
              from n11425144215
              to 1074.7,1765.6
              to 1097.9,1779.3
              to 1117.6,1793.7
            road "Church Street one way" kind=primary-5
              from n246806302
              to n5600204982
              to n5600204989
              to -1261.0,-290.2
              to n246810548
              to n246810549
              to -1130.5,-378.9
              to -1085.6,-416.2
              to n246810545
            road "Oxley Walk" kind=residential-2
              from n1363277802
              to -2367.8,923.4
              to -2374.0,912.7
              to -2374.0,835.3
            road "Tanjong Rhu Road one way 2" kind=tertiary-2
              at 1127.4,1789.5
              to 1093.8,1765.2
              to 1077.5,1756.1
              to 1061.0,1749.3
              to n8229495948 kind=tertiary-3
              to 984.1,1721.2
              to 971.1,1718.6
              to 958.3,1721.4
              to 945.6,1732.5
              to 939.9,1743.5
              to 924.5,1783.3
            """;

    private static final String PART_3 = """
            road "Club Street 2" kind=residential-2
              from n246806303
              to -1410.7,-506.4
              to -1421.1,-519.5
              to n1837448960
              to n1831929820
            road "Union Street" kind=tertiary-2
              from n4242600037
              to -1032.3,-1104.6
              to n8875654200 kind=tertiary-4
              to -962.6,-1124.8
              to -913.4,-1140.0 kind=tertiary-2
              to -905.7,-1143.9
              to n4242600038
            road "Prinsep Link" kind=residential-2
              at -1579.8,1430.0
              to n595254198
            road "Prinsep Street" kind=residential-2
              at -1656.5,1390.0
              to -1646.7,1413.9
            road "China Street one way 2" kind=secondary-1
              from n5600204989
              to n5600204988
              to -1371.8,-383.6
              to n3059542035
              to -1398.2,-484.7
              to n6408411362
            road "Pearl's Hill Road" kind=residential-2
              at -2200.9,-304.3
              to -2199.7,-306.3
              to -2183.4,-302.3
              to -2158.8,-292.2
              to -2122.0,-266.1
              to n1363294008
            road "Duxton Hill 3" kind=residential-2
              from n6038493817
              to n4600790494
              to n4982867037
            road "Nicoll Highway one way 2" kind=trunk-3
              from n2015359296
              to -483.4,1576.2
              to -520.8,1526.9
              to -535.9,1504.4
              to -585.3,1420.1
              to -645.0,1304.7
              to -673.1,1246.1
              to -682.4,1225.8 kind=trunk-4
              to n4602578367 kind=trunk-5
              to n233783186 kind=trunk-3
              to -743.6,1050.5
              to -757.8,989.0
              to -769.8,944.1 kind=trunk-4
              to n233497179 kind=trunk-5
              to -820.1,746.9 kind=trunk-4
              to n233879421 kind=trunk-5
            road "Liang Seah Street one way" kind=residential-1
              from n232119579
              to -950.4,1420.5
              to -861.9,1365.2
              to n233602664 kind=residential-2
            road "Tras Street one way" kind=residential-1
              from n4745192778
              to n6691407338
              to -1542.2,-1359.8
              to n6691407336
              to n6691407335
              to n4745192778
            road "Sheares Link one way 2" kind=residential-2
              from n1832238046
              to n1832238044
              to 27.7,-206.8
              to 36.8,-207.4
              to n4600842969
            road "Marina Gardens Drive 2" kind=residential-2
              at 923.5,-0.5
              to 928.0,17.8
            road "Outram Park" kind=unclassified-2
              from n10765152292
              to -2268.9,-727.2
              to -2275.7,-711.1
              to -2392.9,-661.5
            road "Marina East Drive" kind=unclassified-3
              from n7153058639
              to 2072.2,1464.2
              to 1809.0,1153.1
              to 1761.1,1100.3
              to 1718.1,1049.5
              to 1681.5,1006.4 kind=unclassified-2
              to 1641.7,963.2
              to 1632.2,950.7
              to 1626.0,934.2
              to 1624.7,926.3
              to 1626.1,917.4
              to 1629.7,904.4
              to 1637.1,891.6
              to 1645.6,883.7
              to 1708.7,839.2
              to 1736.5,811.9
              to 1760.8,778.9
              to 1778.1,744.1
              to 1791.2,705.4
              to 1801.1,663.8
              to 1805.2,628.0
              to 1801.6,585.4
              to 1794.0,553.2
              to 1779.2,508.0
              to 1757.8,471.8
              to 1742.3,449.3
              to 1660.3,339.0
            road "Bukit Pasoh Road" kind=residential-2
              from n1836286636
              to n243668557
            road "Duxton Hill 4" kind=residential-2
              from n4745151601
              to n5686639667
            road "residential 7" kind=residential-2
              from n4744279229
              to n4744279230
            road "Loke Yew Street 2" kind=residential-2
              from n5233582710
              to n7152969479
            road "Teo Hong Road" kind=residential-2
              from n4598518199
              to n243668558
              to -2034.5,-971.4
              to -2040.1,-963.9
              to n237170670
            road "Keong Saik Road one way 3" kind=residential-1
              from n1629037271
              to n5231423068
              to -1933.6,-842.4
              to n1629037257
              to -1939.3,-836.2
              to n243668564
            road "Mohamed Sultan Road" kind=residential-2
              from n682933110
              to -2380.9,307.8
              to n682933923 kind=residential-4
              to n74405856
              to -2322.3,486.5
              to n6232047028
            road "Fullerton Square one way" kind=tertiary-2
              from n1149139906
              to -856.4,-89.7
              to -858.0,-84.6
              to -863.8,-76.3
              to -869.4,-73.6
              to n243233999
              to n249766692
            road "Merchant Road one way 2" kind=secondary-3
              from n237218756
              to n564553552
              to n237251482
              to n4723328880 kind=tertiary-2
              to n3756989288
              to -1943.4,27.5
              to -1991.6,44.6
              to -2048.0,62.0
              to n13917491481 kind=tertiary-1
            road "Marina Coastal Drive 2" kind=unclassified-2
              from n13064657905
              to n1904310907
            road "Marina Coastal Drive one way 10" kind=unclassified-2
              from n1985259401
              to n13064657905
            road "Raeburn Park" kind=residential-2
              from n4600811395
              to -2360.6,-1895.9
            road "motorway_link one way 23" kind=motorway-link-1
              from n388569722
              to -2284.7,-1983.6
              to -2313.4,-1993.5
            road "Keppel Road one way 4" kind=trunk-3
              from n4710862651
              to -2027.9,-1864.2
              to n1829822434
              to n246801934
              to n237278991
              to n237278876
              to n237153849 kind=trunk-4
              to n4982866591 kind=trunk-5
              to n4684502273
              to n237154345 kind=trunk-4
              to -1327.3,-1641.6 kind=trunk-2
              to n237154323
            road "motorway_link one way 24" kind=motorway-link-3
              from n2521115159
              to n2082809510
            road "Clemenceau Avenue one way 4" kind=primary-3
              from n4734606543
              to -2092.2,615.8
              to -2087.3,601.1
              to -2080.6,561.3
              to -2080.8,541.3
              to -2084.0,521.1 kind=primary-4
              to n1275666718
              to n1275666714
              to -2137.1,303.9 kind=primary-3
              to -2147.3,253.5
              to -2152.8,226.4 kind=primary-4
              to n1790124158 bridge
              to -2172.5,137.4 kind=primary-2
              to n1275666727 ground
              to n4600634120
              to -2182.9,75.3
              to -2202.2,-10.6
              to n4606287878 kind=primary-3
              to n1790124104
            road "Clemenceau Avenue one way 5" kind=primary-3
              from n4734606540
              to n1363263376
              to n1363263538
              to -2224.0,887.3
              to -2248.5,955.4
              to n6151700895 kind=primary-4
            road "Straits View one way" kind=tertiary-3
              from n1316829262
              to n248420531
              to n1539944086
              to n1539944025 kind=tertiary-2
              to n9913888161
            road "motorway_link one way 25" kind=motorway-link-3
              from n6138767692
              to 837.0,-827.0
              to n2521884271 kind=motorway-link-4
            road "Clemenceau Avenue one way 6" kind=primary-3
              from n1275666710
              to n1275666709
              to n1363263517
              to n4734606543
            road "Clemenceau Avenue one way 7" kind=primary-3
              from n6151700895
              to -2287.5,1071.2
              to n74454975
            road "Clemenceau Avenue one way 8" kind=primary-link-1
              from n6151700895
              to n1275666710
            road "Armenian Street" kind=residential-2
              from n232353088
              to -1556.0,676.5
              to -1566.0,678.7
            road "Bencoolen Street one way" kind=primary-2
              at -1500.4,1455.0
              to n595254198
              to n5765341406
              to -1625.6,1179.6
              to n233497244
              to -1640.6,1155.7
              to -1664.0,1104.6
              to -1673.3,1086.2 kind=primary-3
              to n235129976
            road "residential one way 2" kind=residential-1
              from n621083492
              to n5817301661
            road "Yan Kit Road one way" kind=residential-1
              from n246806266
              to -1746.4,-1314.0
              to -1781.3,-1393.4
              to -1780.9,-1406.3
              to -1756.5,-1543.7
              to -1756.7,-1550.8
              to -1763.9,-1585.2
              to n246806272
              to n1829822469
            road "Keong Saik Road one way 4" kind=residential-2
              from n243668561
              to -1908.8,-1073.9
              to -1901.0,-1089.7
              to -1895.4,-1097.8
              to n26782154
            road "Duxton Road one way" kind=residential-1
              from n6048295590
              to -1742.9,-960.9
              to n4745151601
              to -1706.6,-1053.3
              to -1701.9,-1067.2
              to -1700.1,-1077.9
              to -1699.5,-1085.5
              to -1702.3,-1112.8
              to -1711.1,-1169.3
              to n246800809 kind=residential-2
            road "Tras Street 3" kind=residential-1
              from n472730475
              to n7241390520
            road "Mosque Street one way" kind=residential-2
              from n26781973
              to -1643.1,-494.6
              to -1839.0,-451.1
              to n246800416
            road "Club Street one way" kind=residential-1
              from n246806296
              to n1837448830
              to -1522.2,-597.6
              to -1513.2,-586.9
              to n1831929820
            road "Ann Siang Road one way" kind=residential-1
              from n1318603861
              to n246806296
            road "Banda Street" kind=residential-1
              from n246800769
              to n246800772
            road "Tank Road one way 3" kind=residential-2
              from n1363263413
              to -2170.1,599.7
              to -2171.3,577.0 kind=residential-1
              to -2187.4,518.3
              to -2187.6,508.7
              to n682938046
            road "Amoy Street one way" kind=residential-1
              from n7027677929
              to n7271112667
              to -1380.4,-835.2
              to -1382.4,-832.1
              to -1382.0,-784.9
              to -1377.2,-742.9
              to n26782185
              to n1947625968
              to n1953018747
            road "Carpenter Street one way" kind=residential-1
              from n237170369
              to -1445.4,49.0
              to -1440.2,45.2
              to n237012083
            road "Cheng Yan Place one way" kind=residential-1
              at -1162.6,1541.8
              to -1141.6,1530.9
              to n595268007 kind=residential-2
            road "Purvis Street one way" kind=residential-1
              from n440009224
              to n440009225
            road "Tan Quee Lan Street one way" kind=residential-1
              from n6246027974
              to -900.1,1458.0
              to n6246027973
            road "Telok Ayer Street one way" kind=residential-1
              from n237089604
              to -1266.6,-1035.6
              to -1272.4,-1031.6
              to -1295.5,-1022.4
              to n5233472037
              to -1270.4,-1028.2
              to -1264.7,-1028.1
              to n7828120483
            road "Maxwell Link" kind=residential-2
              from n5094607351
              to -1117.2,-1045.2
              to n9350542655
              to n9350542650
              to n246800870
            road "Magazine Road" kind=residential-2
              from n237251504
              to -2120.0,14.7
              to -2128.2,-4.8
              to -2167.6,-84.1
              to n249693001
            road "Loke Yew Street one way" kind=residential-1
              from n4311466610
              to -1479.7,729.8
              to n5233582710
              to -1480.1,725.6
              to -1532.6,738.1
              to n1857823513
            road "Republic Boulevard one way 9" kind=primary-2
              from n3076643620
              to -40.7,865.9
              to -27.5,876.8
              to -19.7,887.0
              to -13.8,900.1
              to -12.0,910.2
              to -12.9,919.4
              to -25.4,956.5
              to -52.7,1012.1
              to -104.7,1084.3
              to -131.5,1117.6
              to -138.7,1132.5
              to -144.2,1148.0
              to -156.2,1205.4
              to -170.7,1293.3
              to -183.4,1355.8
              to n1783639818 kind=primary-3
            road "residential 8" kind=residential-2
              from n6228532829
              to -1494.5,-1453.0
            road "primary_link one way 23" kind=primary-link-1
              from n4736508655
              to -2307.5,-1328.0
              to -2311.4,-1323.8
              to n7118869386
            road "Raffles Link one way" kind=residential-1
              from n6413515288
              to -749.4,773.0
              to -745.9,787.1
              to -737.9,800.1
              to -721.5,812.9
              to n233879798
            road "residential 9" kind=residential-2
              at -2291.9,-1527.1
              to -2232.1,-1496.9
              to -2198.0,-1483.3
              to n6536886453
            road "residential one way 3" kind=residential-1
              from n1831960747
              to n6575109847
            road "Gardens by the Bay Drop Off one way" kind=unclassified-1
              from n1829374038
              to 590.4,-36.6
              to 591.0,-31.0
              to 594.1,-27.0
              to 610.8,-17.8
              to 619.8,-14.9
              to n1829374182
            road "Marina Grove 3" kind=unclassified-4
              from n12615243863
              to n5069323959
            road "China Street one way 3" kind=secondary-2
              from n6408411362
              to -1401.2,-480.9
              to n5600204998
              to -1375.4,-383.2
              to n3059534130
              to n5600204982
            road "Fort Canning Link one way 3" kind=primary-3
              at -1598.1,879.3
              to -1627.7,888.6
            road "Keppel Road one way 5" kind=trunk-3
              at -2355.5,-1967.8
              to n4710862651
            road "Hospital Drive one way" kind=residential-2
              at -2390.5,-1385.3
              to -2379.8,-1383.0
              to n1684391416
            road "Cantonment Road one way 8" kind=primary-3
              from n237154569
              to n1829822469
              to -1787.4,-1608.5
              to n1318603811
              to -1827.4,-1555.2
              to n4745106067
            road "Tong Watt Road" kind=residential-2
              at -2380.0,324.4
              to n682933923
            road "Supreme Court Lane one way" kind=tertiary-2
              from n232278983
              to -1249.2,325.4
              to -1246.7,330.5
              to -1215.9,451.8
              to n232353783
            road "East Coast Parkway one way 2" kind=trunk-link-2
              from n6309963192
              to -79.9,551.6 bridge
              to -199.1,914.6
              to -209.2,960.1 ground
              to -223.2,997.5
              to -241.3,1037.3
              to n1145428485
              to -328.2,1203.5 kind=trunk-link-4
              to -375.0,1254.6
              to n26782044
              to -447.7,1306.3 kind=trunk-link-3
              to -489.9,1332.0
              to n233782664
              to n4504594293 kind=trunk-link-4
            road "motorway_link one way 26" kind=motorway-link-1
              at 1578.6,1796.5
              to n2521023068
            road "Rhu Cross" kind=residential-2
              at 529.3,1277.9
              to 517.3,1292.9
              to 468.1,1379.7
              to 470.9,1387.0
              to 437.6,1452.4
              to n247008459
              to n11258902323
              to n11376069516
              to 394.1,1529.3
              to 372.5,1554.1
              to 303.7,1621.3
              to 292.1,1634.7
              to 284.2,1645.4
              to 278.1,1658.3
              to n5278629766
            road "Tanjong Rhu View one way" kind=residential-2
              from n247008452
              to 273.4,1712.8
              to 284.0,1714.5
              to 515.3,1783.7
              to 534.4,1791.9
              to 544.4,1792.2
              to 549.5,1790.2
              to 552.5,1786.7
              to n11376108181
              to n11376108182
            road "Solomon Street one way 3" kind=residential-1
              from n4723344719
              to -2053.9,-31.7
              to n4723344715
            road "Waterloo Street one way" kind=residential-1
              at -1365.2,1493.7
              to n5243492221
              to n5243492222
              to n5243492223
              to n233801222 kind=residential-2
              to n4732312054 kind=residential-1
              to -1373.3,1494.5
            road "Telok Ayer Street one way 2" kind=residential-3
              from n246810549
              to -1214.0,-381.2
              to -1220.8,-399.3
              to -1234.6,-457.7
              to n26782342
              to -1271.8,-585.7 kind=residential-2
              to n26782293 kind=residential-1
              to -1307.7,-728.9
              to n7027677929
              to n6365520408
              to n7271112667
            road "Collyer Quay one way 4" kind=primary-2
              from n246800406
              to -1016.1,-411.3
              to n6993992144
              to -950.1,-392.7
              to n237315539
            road "D'Almeida Street one way" kind=residential-1
              from n6993992144
              to -988.1,-377.3
              to -1001.3,-312.9 kind=residential-2
              to -1006.0,-295.8
              to n246810583
            road "Saint Andrew's Road one way" kind=tertiary-2
              from n232353925
              to n4732525705
              to n232376467 kind=tertiary-4
              to -1054.5,710.9
              to -1045.3,720.3
              to n232353226
            road "Temasek Avenue one way 3" kind=residential-2
              from n595497898
              to -454.2,1076.3
              to -502.8,1049.8
              to -517.0,1037.6
              to -537.1,1016.4
              to n595497900
            road "Temasek Avenue one way 4" kind=tertiary-2
              from n408733173
              to -482.8,1114.7
              to -327.5,1074.9
              to -307.8,1065.0
              to -298.0,1055.7
              to -283.8,1035.8
              to -255.4,955.2
              to n4732339357 kind=tertiary-3
              to -235.1,891.9 kind=tertiary-2
              to -230.7,877.1 kind=tertiary-3
              to n6639134876 kind=tertiary-4
              to n233879899 kind=tertiary-3
              to n233879603 kind=secondary-3
            road "Tras Link" kind=residential-2
              from n4745192782
              to n554037804
            road "Wallich Street 3" kind=residential-2
              from n472730475
              to n237105259
            road "Havelock Square one way" kind=residential-3
              from n237218837
              to -1786.7,-250.7
              to -1792.5,-246.0
              to -1855.2,-216.2
            road "Maxwell Road" kind=secondary-4
              from n7012974711
              to n5233472032
            road "Temasek Boulevard one way 4" kind=tertiary-1
              from n26782044
              to -446.1,1287.7
              to -463.3,1289.8 kind=tertiary-2
              to -474.1,1288.2
              to -485.7,1283.3
              to -492.8,1273.0
              to -511.5,1193.9
              to -514.1,1177.6
              to -512.0,1165.0
              to n1363236253
            road "Oldham Lane one way 2" kind=residential-2
              from n4682910150
              to -2068.0,1233.3
              to n408999670 kind=residential-3
            road "Adis Road" kind=residential-2
              at -1846.6,1374.6
              to -1856.3,1356.9
              to -1864.7,1347.3
              to -1877.7,1336.0
              to -1913.5,1316.9
              to n1347466379
            road "Mount Sophia" kind=residential-2
              from n1347466379
              to -1934.0,1313.4
              to -1945.0,1317.5
            road "Merchant Loop one way 3" kind=residential-1
              from n5186902946
              to n74444100
            road "Clarke Quay one way" kind=residential-2
              from n246800568
              to -2028.1,291.3
              to -2031.0,301.7
              to -2027.5,320.5
              to n571492447
            road "Circular Road one way" kind=residential-1
              from n246754375
              to -1339.2,-51.1
              to -1330.3,-62.0
              to -1282.5,-102.5
              to -1255.4,-126.0 kind=residential-2
              to -1251.7,-131.2
              to n246806310
              to -1251.0,-148.6 kind=residential-1
              to n237251040
            road "Circular Road one way 2" kind=residential-1
              from n1398048399
              to n1398048318
              to -1411.3,90.0
              to n7904178876
            road "Smith Street one way 3" kind=residential-1
              from n7027603394
              to -1799.9,-641.5
            road "Park Crescent" kind=residential-2
              from n249690365
              to -2008.8,-568.9
              to -2013.4,-565.1
              to -2016.0,-559.5
              to -2031.6,-494.0
              to -2030.4,-484.6
              to -2022.0,-460.9
              to -1995.3,-400.5
              to n237296520
            road "Park Crescent one way" kind=residential-1
              from n237218696
              to -1977.6,-583.9
              to n249690365
              to -1975.7,-579.4
              to n7027603403
            road "Teck Lim Road one way" kind=residential-1
              from n246800493
              to n26782248
            road "Kreta Ayer Road one way" kind=residential-2
              from n26782339
              to -1795.4,-959.6
              to -1800.5,-958.8
              to -1805.2,-955.9
              to -1816.2,-941.9
              to -1870.5,-885.6
              to n5231423068
              to n243668564 kind=residential-3
              to n237154127
              to n237218865
            road "Boon Tat Street" kind=residential-2
              from n26782185
              to n26782293
            road "residential 10" kind=residential-2
              from n7030663130
              to n7030663129
            road "residential 11" kind=residential-2
              from n7030663132
              to n7030663131
            road "Handy Road one way 2" kind=residential-2
              from n7043034841
              to -2038.3,1262.6
              to -2016.9,1251.7
              to -2003.9,1247.7
              to -1980.9,1243.9
              to -1926.0,1241.3
              to -1917.6,1239.6
              to -1907.6,1236.6
              to -1894.6,1223.8
              to -1882.5,1201.7 kind=residential-3
              to n7043034842
              to n7043034847 kind=residential-2
            road "Oldham Lane one way 3" kind=residential-1
              from n7043034841
              to n4684627247
              to -2069.8,1264.0
              to -2070.9,1254.5
              to n4682910150
            road "Keng Cheow Street one way" kind=residential-1
              from n4723328880
              to n4724301488
              to n14250610564
              to n4724301590
              to n4724301592
              to n4724301594
              to n4723328877
            road "Keng Cheow Street one way 2" kind=residential-1
              from n4600694846
              to n4724301593
              to n4724301591
              to n4724301589
              to n14250610565
              to n4724301487
              to n3756989288
            road "Connaught Drive 2" kind=residential-1
              from n3010901912
              to n6856578455
            road "Wallich Street one way 2" kind=residential-1
              from n7097470164
              to -1354.8,-1088.4
              to -1359.5,-1089.4
              to n7097470158
              to n4724923626 kind=residential-2
              to n241595239
            road "East Coast Parkway one way 3" kind=motorway-4
              from n6309963192
              to -97.4,666.5 bridge
              to -116.8,765.4
              to -132.1,828.9
              to -138.1,860.3
              to -142.3,912.7
              to -140.5,954.1
              to -137.5,976.9
              to -131.5,1002.4
              to -113.2,1043.3
              to -87.9,1085.5
              to -68.4,1109.6
              to -45.8,1131.7
              to -22.6,1150.4
              to 3.9,1169.1
              to n26782016
              to 707.3,1538.4 kind=motorway-5
              to 767.2,1566.7 ground
              to n243494149
              to 999.7,1643.1 kind=motorway-3
              to n25455287 kind=motorway-4
              to 1098.2,1673.8 kind=motorway-3
              to 1204.0,1708.7
              to 1397.1,1774.3
              to 1478.7,1799.1
            road "Raffles Boulevard one way" kind=primary-5
              from n233497179
              to -766.4,844.3
              to n233879798 kind=primary-6
              to n233880373
              to n595497945 kind=primary-5
              to -477.7,774.4
              to -444.9,772.2
              to -393.3,780.1
              to -308.4,799.6
              to n4600854642 kind=primary-6
              to n233879872 kind=primary-5
              to n233879899
              to n4726227141 kind=primary-3
              to -139.7,841.4
              to -99.2,847.8
              to n3076643620 kind=primary-4
              to -18.8,856.7 kind=primary-2
              to 0.3,853.3
              to n13698051636
            road "Marina Way one way" kind=tertiary-2
              from n9913888157
              to n1539944070
              to -527.4,-906.2
              to n1539944050
              to n1629524122 kind=tertiary-3
              to -458.6,-661.8
              to n1539883528
            road "unclassified" kind=unclassified-2
              from n2879240169
              to 109.5,-1556.5
              to 89.4,-1559.2
            road "Armenian Street 2" kind=residential-2
              from n232353088
              to n1857823513
            road "Sago Lane" kind=residential-1
              from n246800769
              to -1716.2,-762.3
            road "Craig Road one way" kind=residential-1
              from n26782154
              to n7030663132
              to n7030663130
              to -1793.9,-1181.3
              to -1778.0,-1196.9
              to n7030292675
              to -1767.6,-1206.3
              to -1779.2,-1199.2
              to -1795.4,-1183.2
              to n7030663129
              to n7030663131
              to n7030292676
            road "Erskine Road one way" kind=residential-1
              from n26782131
              to -1621.1,-744.6
              to -1594.7,-756.2
              to -1575.8,-770.2
              to -1538.8,-817.6
              to n246806289 kind=residential-2
            road "Marina View" kind=tertiary-3
              from n1539897211
              to -902.1,-1157.9
              to n4242600038
            road "Chin Swee Road one way 4" kind=primary-2
              from n4606287879
              to -2248.7,-218.8
              to -2262.9,-241.6
              to -2327.5,-318.2
            road "Chin Swee Road one way 5" kind=primary-2
              at -2386.4,-339.8
              to -2314.0,-260.4
              to -2272.3,-205.6
              to n1790098230 kind=primary-3
            road "River Valley Road one way 5" kind=primary-4
              from n74442029
              to n11359537763
              to n1275666714
              to n74454952
              to n74407251 kind=primary-3
              to -2245.1,497.2 kind=primary-2
              to -2277.4,510.3
              to n74392496
              to n6232047026
              to -2334.0,550.3
              to n8167517154
            road "Beach Road one way 2" kind=secondary-3
              at -581.2,1726.0
              to n232083078
              to -698.9,1541.6
              to n232083643 kind=secondary-4
              to -778.2,1401.4 kind=secondary-3
              to -821.6,1316.9
              to n4602578374
              to n233801368
              to n4732407688
              to -886.6,1131.0
              to n1847853747 kind=secondary-4
              to -908.1,1055.5 kind=secondary-3
              to -948.6,912.5
              to -949.0,905.7
              to n595254399
            road "Middle Road one way 2" kind=secondary-4
              at -1425.0,1482.6
              to n4732312054
              to n5243492221
              to -1315.6,1436.4 kind=secondary-3
              to n4602578386 kind=secondary-4
              to -1222.1,1398.1 kind=secondary-3
              to n233432542 kind=secondary-4
              to n4602578375 kind=secondary-3
              to -1096.1,1343.0
              to n4602578377 kind=secondary-4
              to -901.4,1263.6 kind=secondary-3
              to -882.1,1256.1 kind=secondary-4
              to n4602578383
              to n4602578385 kind=secondary-3
              to n4602578374
              to -849.5,1212.1
              to -845.9,1207.7 kind=secondary-2
              to -823.5,1197.7
              to n4602578373 kind=secondary-3
              to n4602578367
            road "Tank Road one way 4" kind=residential-1
              from n1363263376
              to n1363263536
            road "Anson Road one way 3" kind=residential-2
              from n6897440292
              to -1456.0,-1697.3
              to n6897440293
              to n1831960718
              to -1455.6,-1737.6 kind=residential-1
              to n6575109847
              to -1506.5,-1830.1
              to -1516.5,-1849.2 kind=residential-2
              to -1544.4,-1899.1 kind=residential-1
              to -1557.2,-1891.9
              to -1544.6,-1867.5
              to -1519.7,-1829.7
              to n1831960747
              to n1831961127
              to -1483.0,-1762.1 kind=residential-2
              to -1465.0,-1724.2
              to n246806260
              to n6897440292 kind=trunk-2
            road "Kadayanallur Street one way" kind=residential-1
              from n7705372942
              to n7705372941
              to -1546.0,-861.3
              to n7705372943
              to -1542.7,-863.9
              to n4745272384
              to n4745272383
            road "Kadayanallur Street" kind=residential-2
              from n7705372943
              to n246806289
            road "secondary_link one way 3" kind=secondary-link-1
              from n1363263232
              to -2288.9,854.0
              to n1363263545
            road "primary_link one way 24" kind=primary-link-1
              from n1363263485
              to -2138.5,831.7
              to -2147.4,769.6
              to n1363263517
            road "Fort Canning Road one way 3" kind=secondary-2
              from n1363263434
              to -2093.0,933.9
              to -2103.1,921.8
              to n243234023
            road "Tras Street one way 2" kind=residential-1
              from n7241390520
              to -1581.2,-1030.9
              to -1582.3,-1027.9
              to n1814187447
            road "residential one way 4" kind=residential-1
              from n5817301661
              to n3059598143
            road "secondary_link one way 4" kind=secondary-link-1
              from n1318569081
              to -1292.1,-1091.5
              to -1280.0,-1081.8
              to n1318544675
            road "Straits Boulevard one way 2" kind=secondary-3
              from n4602143859
              to -1006.3,-1241.9
              to n4724923624
            road "Dickenson Hill Road one way" kind=residential-1
              from n246800789
              to n8153290963
            road "residential 12" kind=residential-2
              from n8167517153
              to n8167517154
              to -2385.1,588.1
            road "Cross Street one way" kind=primary-4
              from n248527331
              to n246759045
              to n249766698
              to n26782230
              to n26782342 kind=primary-5
              to n1953018747
              to n6552271865
              to n246806303
              to n237012179
            road "Marina South Drive" kind=unclassified-2
              from n2519916627
              to 1198.0,-193.6
              to 1260.3,-227.4
              to 1270.4,-239.4
              to 1273.3,-246.8
              to 1274.7,-256.1
              to 1272.9,-267.8
              to 1236.3,-318.1
              to n2653101281
              to n4712021480
            road "Hospital Drive one way 2" kind=residential-2
              from n5138586950
              to -2393.2,-1402.2
            road "Shenton Lane 2" kind=tertiary-4
              from n2598074292
              to n6353451366
              to n8875654200
            road "Kampong Bahru Road one way 2" kind=primary-4
              at -2388.5,-1416.2
              to n5138586950
            road "Upper Cross Street one way 3" kind=primary-4
              from n5233639468
              to n1363293957
              to -2068.6,-163.1 kind=primary-3
              to -2080.3,-158.2
              to -2132.8,-147.0
              to n237296721 kind=primary-4
              to -2200.6,-131.8 kind=primary-6
              to n1790098242
              to n237296820
            road "Upper Cross Street one way 4" kind=primary-link-1
              from n5233639468
              to -2050.6,-168.7
              to -2046.5,-156.6
              to -2040.0,-144.1
              to -2033.6,-136.4
              to -2017.2,-127.3
              to n243208171
            road "Bayfront Avenue" kind=secondary-4
              from n3984996251
              to n5187331770
            road "Bayfront Avenue one way 3" kind=secondary-2
              from n5187331770
              to n11389450853
              to n1633135168
            road "residential one way 5" kind=residential-1
              from n9350542650
              to -1149.6,-1148.9
            road "residential one way 6" kind=residential-1
              at -1143.9,-1127.1
              to n9350542655
            road "Connaught Drive one way 2" kind=residential-1
              from n4451336954
              to -1047.7,246.1
              to n9370421301
              to n4451336941
            road "Connaught Drive one way 3" kind=residential-1
              from n6856578454
              to n6856578455
              to -1157.0,297.7 kind=residential-2
              to n232353925
            road "McCallum Street one way 2" kind=secondary-3
              from n9489438159
              to -1025.7,-908.5
              to -1031.2,-909.4
              to n5094607351
              to n237055163
              to n621083492
              to n5817288340
              to n237089541
            road "Outram Park one way" kind=unclassified-2
              from n7852537777
              to -2354.8,-883.7
              to -2350.9,-880.0
              to -2340.2,-841.3
              to -2332.5,-825.0
              to -2305.4,-793.8
              to -2282.0,-766.8 kind=unclassified-1
              to n10765152292
              to -2276.5,-773.4
              to n5187147003
              to -2321.7,-829.5 kind=unclassified-2
              to -2328.0,-841.5
              to -2337.0,-877.8
              to -2335.8,-892.5
              to n9559521797
            road "Marina Coastal Drive one way 11" kind=unclassified-1
              from n6528381380
              to n6528381379
            road "Marina Coastal Drive one way 12" kind=unclassified-1
              from n6528381381
              to n1985259401
            road "Eu Tong Sen Street one way" kind=primary-5
              from n5138586950
              to n1684391416
              to n7118869386
              to -2277.7,-1278.9
              to -2198.4,-1182.4 kind=primary-4
              to -2181.9,-1158.0 kind=primary-5
              to -2171.9,-1138.9
              to n1829822445 kind=primary-6
              to n237348741 kind=primary-5
              to n237218779
              to n1829822462 kind=primary-4
              to n5172094036
              to -2040.2,-841.8
              to n237218865
              to n1852241866
              to n237218696
              to n7027603403
              to -1874.2,-462.8
              to n5165895535 kind=primary-6
              to n237219117 kind=primary-5
              to n237218837 kind=primary-4
              to n237219180 kind=primary-5
              to -1738.2,-161.1 kind=primary-4
              to n237218756 kind=primary-5
              to n74460245
              to n2750379978 kind=primary-4
              to -1651.9,26.5
              to n1816580034 kind=primary-5
              to -1576.8,198.3 kind=primary-4
              to n5187250342 kind=primary-5 bridge
              to -1541.4,285.4 kind=primary-4
              to n232207994 ground
            road "trunk one way" kind=trunk-1
              from n13812791644
              to -825.6,-8.2
              to n1951506322
            road "residential one way 7" kind=residential-1
              from n9987737918
              to -2271.0,-1590.8
              to n9987737924
            road "residential one way 8" kind=residential-1
              from n9987737920
              to -2273.0,-1587.7
              to n9987737924
            road "residential one way 9" kind=residential-1
              at -2275.8,-1582.0
              to n9987737922
            road "Parsi Road one way" kind=residential-2
              from n7008336140
              to -1321.0,-1413.8
              to -1313.8,-1412.6
              to -1294.0,-1416.1
              to n554037752
              to -1221.5,-1448.1
              to -1213.4,-1452.8 kind=residential-3
              to n246806248
            road "Tanjong Rhu Road one way 3" kind=tertiary-2
              at 861.1,1792.3
              to 820.6,1781.7
              to 769.1,1771.6
              to 743.6,1764.1
              to n11376108182 kind=tertiary-1
              to 545.9,1693.7
              to 524.8,1680.6
              to 512.7,1668.5
              to 463.5,1595.4
              to 426.2,1531.5
              to 411.7,1509.0
              to n11258902323
            road "Clarke Quay" kind=residential-2
              from n4672154348
              to n246800568
            road "Chulia Street one way" kind=tertiary-2
              from n249766690
              to n246810568
              to -1169.6,-174.9
              to n246810582
              to n4616343182
              to n237251040
            road "Ann Siang Road 3" kind=residential-2
              at -1447.2,-799.9
              to n1318603861
            road "Connaught Drive one way 4" kind=residential-1
              from n4451336941
              to -1031.4,255.5
              to -1025.3,252.9
              to -1023.3,250.2
              to -1022.5,243.1
              to -1027.2,237.8
              to -1030.8,236.7
              to -1034.5,237.0
              to n4451336954
            road "trunk one way 2" kind=trunk-1
              from n11259104759
              to n237054768
            road "Tanjong Rhu Road one way 4" kind=tertiary-1
              at 1017.0,1769.4
              to n11424995020
              to 1026.0,1742.8
              to n8229495948
            road "tertiary_link one way 4" kind=tertiary-link-1
              from n11424995020
              to 1030.0,1751.3
              to n11425144215
            road "primary_link one way 25" kind=primary-link-1
              from n11359537763
              to -2049.1,445.9
              to n5794881171
            road "Marina South Avenue one way" kind=tertiary-2
              from n1985259425
              to 709.9,-246.9
              to 714.8,-261.9
              to 717.3,-277.5
              to 717.3,-291.9
              to n11465244446
              to n12615243874
              to 666.4,-465.3
              to n6306233270
              to 503.9,-1024.4
              to 502.3,-1027.9
              to 495.3,-1029.6
              to 490.3,-1027.6
              to 487.0,-1023.2
              to 486.3,-1019.3
              to n6094644741
              to n12615243873
              to n11465244435
              to 708.4,-290.9
              to 706.1,-268.4
              to 701.7,-252.3
              to n3772852905
            road "Tanjong Rhu Road one way 5" kind=tertiary-1
              from n11376069516
              to 412.7,1522.2
              to 462.7,1604.1
              to 505.3,1667.9
              to 531.5,1691.0
              to n11376108181
              to 742.3,1769.8
              to 859.3,1798.9 kind=tertiary-2
            road "Park Street one way 2" kind=secondary-2
              from n9913888164
              to n1633135168
              to n11389450854
              to n9913888157
              to n9913888161
              to n9913888162
              to n6353451366
              to -1019.1,-919.0
              to -1022.3,-919.2
              to n9489438156
            road "trunk_link one way 9" kind=trunk-link-2
              from n12071463880
              to -870.3,638.3
              to n12071463879
            road "Republic Boulevard one way 10" kind=trunk-link-2
              from n1538307248
              to -269.9,1371.1
              to -304.5,1313.0
              to -313.3,1293.8
              to -317.1,1281.7
              to -315.9,1274.5
              to n12687053274
            road "Mohamed Ali Lane one way" kind=residential-1
              from n5233472026
              to -1518.2,-556.5
              to -1535.0,-564.0
              to n1837448982
            road "Marina Boulevard one way 3" kind=primary-5
              from n237054972
              to n248527325
              to n4724570673
              to -701.1,-484.6 kind=primary-4
              to n5282854973 kind=primary-5
              to n1316829262
              to n1539883528
              to n1539883491
              to n1539883582
              to n1145466122
              to n1539944100
              to n1629337146
              to n1629337142 kind=primary-2
              to n6094644741 kind=primary-5
              to n6306233270 kind=primary-4
              to n2521115176 kind=primary-5
            road "motorway_link one way 27" kind=motorway-link-2
              from n1539835341
              to -507.4,-1377.1 bridge
              to -513.7,-1377.9
              to -555.2,-1375.9
              to -595.1,-1376.9
              to -616.7,-1379.8
              to -653.5,-1388.5
              to -675.8,-1394.4 ground
              to -701.6,-1397.3
              to -731.9,-1395.0
              to -746.4,-1392.1
              to -768.6,-1378.0
              to n2598074281
            road "residential 13" kind=residential-2
              at 408.0,1455.0
              to n247008459
              to 444.9,1473.2
            road "Ophir Road one way 3" kind=trunk-2
              from n12741088964
              to -349.0,1281.2
              to -345.6,1276.4
              to -340.5,1271.9
              to -322.4,1262.6
              to -313.9,1258.4 kind=trunk-link-2
              to -307.9,1253.1
              to -290.1,1229.2
              to -230.2,1165.0
              to -161.1,1109.7
              to -123.5,1074.2
              to -90.8,1035.6
              to -65.1,995.8
              to -62.1,987.5
              to n234839820
            road "Marina Coastal Drive one way 13" kind=unclassified-1
              from n5006326669
              to 215.4,-1693.3
              to n1904310907
              to 216.7,-1698.1
              to n5006326665
            road "Marina Coastal Drive one way 14" kind=tertiary-1
              from n4602213163
              to n4602213166
              to 258.9,-1610.5
              to n2879240177
              to n2879240165
              to 302.9,-1551.7
              to n2879240173
              to 382.9,-1450.4 kind=tertiary-3
              to 396.1,-1437.3
              to 422.2,-1417.1
              to 495.7,-1366.2
              to 519.2,-1345.9
              to 542.9,-1321.5
              to 672.2,-1155.0
              to 676.3,-1146.0
              to 680.8,-1127.2
              to 676.6,-1111.2 kind=tertiary-4
              to n1539883493
              to 664.2,-1099.5
              to 654.8,-1095.4
              to n2521884269
            road "Raffles Avenue one way" kind=primary-2
              from n233879721
              to n13698051636
              to 24.2,822.4
              to 18.1,700.9
              to 11.9,685.1
              to 4.0,672.8
              to -7.0,665.2 kind=primary-3
              to -44.8,657.8
              to -141.4,634.4
              to n233879603 kind=primary-4
              to n479983003 kind=primary-3
              to n5165925965 kind=primary-4
              to -556.7,537.6
              to -586.6,533.4
              to -609.9,533.6
              to -624.7,536.0
              to -639.7,540.6
              to -689.7,564.2
              to n233879440 kind=primary-5
              to -786.4,607.9 kind=primary-4
              to n4726775783 kind=primary-6
              to n233879421 kind=primary-5
            road "Marina Coastal Drive one way 15" kind=unclassified-1
              from n13770297460
              to 437.2,-1769.1
              to 468.3,-1793.9
              to 470.9,-1799.3
              to 469.8,-1805.1
              to 445.1,-1835.7
              to 440.4,-1837.3
              to 434.2,-1837.4
              to 402.6,-1813.9
              to n13770297457
            road "Marina Coastal Drive one way 16" kind=unclassified-1
              from n13770297447
              to 521.3,-1831.4 bridge
              to 483.9,-1880.1
              to 478.7,-1882.1
              to n13770297452
            road "Marina Coastal Drive one way 17" kind=unclassified-1
              from n13770297452
              to 428.0,-1848.4 bridge
              to 367.7,-1811.9
              to n1985259401 ground
            road "Marina Coastal Drive one way 18" kind=unclassified-1
              from n6528381379
              to 432.9,-1751.8
              to n13770297460
            road "Marina Coastal Drive one way 19" kind=unclassified-1
              from n13770297460
              to 448.2,-1766.3
            road "Marina Coastal Drive one way 20" kind=unclassified-1
              from n13770297457
              to 377.7,-1809.3
              to n6528381381
            road "Marina Coastal Drive one way 21" kind=unclassified-1
              at 403.0,-1823.2
              to n13770297457
            road "tertiary" kind=tertiary-4
              from n9913888161
              to -776.8,-1074.1
            road "tertiary one way" kind=tertiary-1
              from n13917491481
              to -2065.3,68.9
              to -2068.2,78.0
              to -2066.2,86.2
              to n1790158061
            road "Merchant Road one way 3" kind=tertiary-1
              from n1790158061
              to -2059.3,92.7
              to -2050.1,92.7
              to n74444085 kind=tertiary-2
              to n74444100
              to -1939.9,55.1
              to -1858.2,15.4
              to n1790324627
              to -1775.7,-25.6 kind=secondary-3
              to -1747.6,-35.3
              to n74460245 kind=secondary-4
            road "residential 14" kind=residential-2
              from n14250610564
              to n14250610565
              to -1948.2,-21.4
            """;

}
