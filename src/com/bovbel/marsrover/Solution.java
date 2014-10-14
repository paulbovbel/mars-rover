package com.bovbel.marsrover;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Problem:
 * You're downloading a large file from a distant locations, of size N bytes. Your connection has a latency of L seconds,
 * and B bytes/second. The file is broken up into C chunks, which have redundant data.
 *
 * Cost to download a chunk is 2 * L + N / B
 *
 * Input format:
 * N - Number of bytes
 * L - Latency
 * B - Bandwidth
 * C - Number of chunks
 * Start, end (chunk 1)
 * Start, end (chunk 2)
 * ...
 * Start, end (chunk n)
 *
 * Datastructures:
 * - Store chunks in a centered interval tree for fast lookup
 * - Record costs of partially solved searches in a red-black tree map
 * - Complexity discussed in each class (as relevant)
 * 
 * General approach:
 * - Problem is a weighted set cover problem http://en.wikipedia.org/wiki/Set_cover_problem.
 * - Starting from position 0, lookup N chunks that include position, and create N new search branches
 * - At each search iteration, prune branches that incur a higher cost than recorded by other branches at the same position
 *   or further ahead search
 * - If branch is found to be a viable branch (lower than all competing solutions), erase any records of higher cost solutions
 *   at the same position or further behind, to keep records tree a manageable size
 *   
 * Notes:
 *
 * - Tree construction and solution search is multi-threaded, tree query is recursive so could technically overflow stack. Tree is
 *   created balanced, and not modifiable after construction, so overflow unlikely for data within limitations provided.
 * - Could have used generics but Java makes this difficult when doing math, tried to keep autoboxing to a minimum.
 * - Keeping track of each branch's chunk set technically unnecessary for solution, but felt like cheating to leave this out.
 * 
 * @author Pavel
 *
 */
public class Solution {

	public static void main(String[] args) {
		
//		String params = "2000\n"+
//						"15\n"+
//						"10\n"+
//						"7\n"+
//						"0,200\n"+
//						"200,400\n"+
//						"400,600\n"+
//						"600,800\n"+
//						"800,1000\n"+
//						"1000,2000\n"+
//						"0,1800";
		
		
//		String params = "2000\n"+
//						"5\n"+
//						"10\n"+
//						"7\n"+
//						"0,200\n"+
//						"200,400\n"+
//						"400,600\n"+
//						"600,800\n"+
//						"800,1000\n"+
//						"1000,2000\n"+
//						"0,1800";

		//2^16 bytes
//		String params = "65536\n5\n10\n1000\n53394,59362\n8322,59859\n6392,41442\n18251,35840\n62752,63235\n10329,63609\n31810,62729\n9298,52447\n27640,60014\n51918,62882\n2340,42975\n55649,61211\n44482,49660\n25705,48702\n11219,42958\n2086,46272\n3025,18148\n6365,53966\n20781,45536\n2257,62274\n25006,28753\n50169,52115\n12247,32097\n29202,42357\n46489,49459\n18089,44545\n10657,42933\n7798,32661\n22307,62898\n14667,38356\n16718,49235\n33158,45815\n58387,62869\n9085,35862\n9784,16876\n16664,55098\n15959,53365\n22936,60901\n12884,16455\n31017,40373\n23046,54450\n36027,38356\n18733,60110\n49397,49624\n24933,37213\n3535,4971\n34786,51064\n8513,61212\n30762,37278\n780,22094\n10628,52055\n20396,34638\n10856,39452\n17234,42866\n45169,49031\n5493,29527\n15006,59857\n9986,54121\n35281,65283\n5123,29011\n6989,63039\n303,50785\n53563,56931\n5533,26200\n17031,52434\n28273,59681\n11917,17288\n8917,9538\n37992,56970\n9499,36036\n40767,55905\n23000,33636\n4978,26333\n8081,15723\n12052,15725\n3254,27346\n59161,61918\n32064,32169\n22133,58986\n7287,24199\n25542,51135\n15839,26471\n6321,8649\n61739,62662\n3917,37697\n15386,23144\n1009,53818\n2819,11075\n42541,47954\n29552,42451\n19419,35849\n12383,48804\n12026,45009\n24149,41001\n5316,51133\n50837,60909\n28564,31902\n20077,29280\n33326,33474\n52090,53584\n24812,42226\n34919,53188\n22985,61539\n36055,57406\n38473,40795\n13614,19742\n15105,30862\n12764,55333\n11187,14806\n14920,28554\n20388,60515\n12112,28194\n59303,64209\n7282,28762\n16912,26786\n17184,38987\n39508,46610\n7695,14532\n19443,20891\n27798,33283\n5604,17202\n1915,52496\n47863,60874\n32021,37914\n15550,30071\n35836,63117\n15178,34153\n32040,40899\n25920,44508\n24080,64749\n2473,58011\n52179,59854\n6469,17162\n21978,44547\n8949,47267\n6996,42845\n32386,51056\n46861,59227\n21900,58388\n12963,45793\n2001,48764\n31452,32769\n39968,59292\n40480,56325\n37796,52789\n11988,15724\n1879,58099\n11005,32106\n46707,64139\n30873,32799\n3907,44694\n2780,4682\n6339,34187\n53579,53619\n9821,47346\n33987,43228\n42532,63765\n29740,52451\n28337,54088\n5470,8727\n11363,25620\n52650,54486\n3963,26166\n27315,34529\n41155,43048\n19135,28289\n1014,64492\n6961,10955\n12984,24406\n22249,32092\n60315,62367\n3452,48357\n17637,27711\n35905,61784\n27377,64426\n19756,45947\n35332,43669\n43682,45751\n8389,11674\n11214,65476\n2136,36779\n43855,57794\n12480,24177\n30194,64333\n10250,56068\n24659,42255\n12512,28066\n7904,31590\n14823,38634\n25206,38207\n16502,19034\n17385,40442\n54027,64400\n22536,47858\n7062,38278\n57649,59396\n17087,53593\n1475,38952\n20494,27870\n10583,11715\n6175,27714\n30862,39225\n45610,45868\n2202,41847\n4509,20945\n34791,42890\n26714,53739\n47079,63482\n21309,34822\n6922,40040\n27751,51040\n5952,17463\n10070,18416\n28841,34547\n29978,57369\n33951,61842\n41793,62764\n15775,44311\n18944,44028\n4456,45557\n14682,16698\n43767,55338\n22575,51152\n440,44259\n25347,39464\n75,60031\n27810,30307\n30207,50473\n21133,51429\n2343,30891\n11526,47301\n10008,31030\n22356,39806\n12566,48394\n15915,60125\n17633,50168\n12364,18841\n5971,37763\n35822,44785\n27900,42234\n42442,44500\n41667,61943\n13692,46484\n7824,15481\n29500,39800\n30063,43381\n22952,50482\n27273,43386\n54586,55177\n16806,40204\n35438,38158\n17352,57013\n7812,20845\n42307,61593\n31422,41898\n35699,42422\n35644,47255\n34242,65124\n6933,14331\n4167,7189\n26514,29385\n23974,50037\n41150,50593\n61136,63750\n9101,12584\n6148,45631\n34433,34757\n31775,56436\n25785,44003\n34082,48579\n9830,22788\n17180,38410\n2913,49476\n15911,28993\n23542,45076\n25867,48257\n44789,46141\n1283,28987\n21683,27807\n12914,17712\n28175,53853\n25636,58181\n26004,50405\n49485,52987\n14157,24733\n51800,62214\n21467,43992\n28747,54625\n10961,50388\n56491,64873\n33713,57953\n10142,38537\n13098,26670\n49067,54106\n20875,51771\n5895,35000\n7320,8932\n32452,44476\n12433,32441\n3602,9673\n36737,55753\n45657,60923\n38194,53438\n57607,64810\n34,56718\n40145,64878\n31426,34582\n14932,52517\n32643,59039\n37661,55390\n38403,48408\n16170,43674\n5471,41023\n43316,47825\n58377,64377\n38106,50399\n38017,60838\n1113,7920\n31739,56539\n13723,55369\n36195,41280\n2096,40286\n3246,23751\n12616,32084\n8066,13467\n9602,12391\n2795,41628\n18472,35298\n32710,45558\n29175,35114\n8122,32136\n55902,57274\n13661,17714\n37027,41964\n13499,27330\n5378,62124\n6927,9308\n10909,40695\n3413,37599\n47754,61028\n4155,48355\n56390,61238\n56292,64514\n33645,51483\n11639,26122\n2024,8777\n19746,61548\n19368,21819\n30610,42480\n1653,55195\n36637,55975\n22798,29231\n3554,11607\n21681,43438\n7743,58884\n35388,64777\n46329,65503\n18864,27166\n30464,50067\n6568,53622\n11673,23569\n3716,34202\n11512,22010\n13693,59321\n30702,44263\n6816,59778\n48252,48860\n12071,36822\n19656,39139\n8790,13933\n4682,58651\n3522,15891\n870,28949\n12888,58799\n6119,20143\n6663,29888\n21764,65234\n4066,19487\n3037,19546\n33124,49901\n5891,41358\n5299,50938\n34981,59319\n7153,54121\n19266,22157\n677,48911\n3175,43773\n34479,39549\n46351,47822\n18873,51209\n36482,45386\n4036,25986\n22124,51130\n39837,48579\n6869,8381\n31800,36015\n52361,58359\n3364,48126\n4776,5801\n52321,61801\n8656,44808\n7232,47365\n7700,41990\n21549,42848\n38220,49095\n15389,48499\n48166,63610\n5651,56816\n24015,24196\n39187,44894\n24094,51732\n5679,13502\n13479,50590\n25446,36161\n15004,42070\n9951,31751\n6593,51245\n15556,19272\n5996,34791\n6871,26563\n7358,51409\n19108,39553\n28343,63205\n45531,49683\n28354,42959\n7193,61195\n12285,17444\n31956,52287\n25953,50395\n2440,17887\n28152,44125\n29605,39968\n3893,20697\n45642,50641\n8213,8529\n512,6052\n27729,42964\n34813,47378\n7131,41404\n8290,8801\n6461,9308\n11026,12861\n20737,20806\n14258,16452\n46087,58519\n12087,36421\n5069,13895\n46315,59887\n20541,36555\n10892,40796\n11169,64746\n16894,26005\n4849,44833\n26371,64412\n26357,40676\n10116,24992\n10560,49684\n22988,57090\n19277,44927\n34775,54554\n21975,39157\n19610,29661\n23567,27698\n36590,48664\n27809,28138\n1601,8183\n19017,20809\n42840,62714\n30008,61324\n15760,50063\n48539,49764\n6941,48739\n30360,44667\n6456,13904\n11469,53974\n10719,43646\n33853,58615\n10065,46053\n35447,62486\n2396,44547\n49062,53032\n7876,34409\n21354,35812\n26141,27203\n11845,16737\n1345,60534\n42841,61120\n10716,60365\n37840,52079\n16883,28838\n14986,49280\n4206,50288\n43988,46872\n27463,42078\n25609,53487\n20803,53382\n51713,55854\n33137,41659\n29096,62318\n3933,56804\n23270,41366\n14691,65340\n39649,42759\n9318,25378\n1647,27598\n12065,47565\n24272,55153\n37423,48119\n11590,62744\n17388,60594\n14665,24482\n5734,41951\n2952,11837\n22770,47394\n25157,43294\n1418,41114\n52466,59676\n48880,53288\n25120,40454\n34737,37716\n16294,18027\n14923,29599\n52721,64626\n1965,35105\n5706,52566\n4387,64825\n1191,61565\n44816,51363\n35005,58023\n41022,58918\n9035,14274\n2740,11936\n7008,40399\n23229,61582\n26911,64511\n44345,61970\n50255,64770\n22066,43410\n16001,19366\n34593,44576\n26974,39495\n38243,49186\n36162,38245\n5412,33543\n47158,65285\n23235,63653\n22705,58101\n27094,29799\n8235,14269\n20245,47586\n45468,51307\n642,55261\n50526,60446\n2795,24785\n46160,47810\n14698,17633\n31293,44108\n15495,40876\n11608,54372\n50261,61242\n7070,11942\n6494,32097\n12664,58714\n2894,6494\n36523,50627\n11729,20443\n13772,22214\n33433,59400\n6654,41217\n3579,25615\n28293,32852\n53189,65377\n31828,58619\n9014,25559\n60129,60776\n40523,46765\n22498,61344\n8177,47880\n42368,54602\n26102,49141\n21133,54737\n36193,64169\n21654,36000\n23635,40598\n27125,49579\n32266,45531\n21480,63750\n48436,54907\n2092,62533\n23388,43428\n15098,18448\n40932,46605\n38706,43283\n3116,22858\n15788,29579\n46861,56111\n18449,47910\n9028,54836\n9083,38549\n23996,52872\n32086,33016\n23143,57479\n29455,63146\n2772,63764\n12400,43721\n38433,44244\n23660,40651\n1262,53160\n5496,63885\n15154,42687\n7996,26443\n16898,17592\n9976,21736\n7973,22807\n6178,57944\n26150,60952\n3106,22438\n48233,52081\n35711,44973\n3590,58566\n3027,19901\n12810,47197\n47301,57528\n4632,38170\n52453,60473\n18740,35630\n46903,64539\n28394,54983\n30843,36747\n17635,49088\n33023,42389\n9091,20168\n23754,31167\n51138,51650\n8749,43812\n1412,36690\n19714,61566\n18784,64285\n52483,58728\n39160,57935\n35990,61849\n37799,47736\n1694,29264\n34158,42356\n24400,61416\n54365,55646\n24414,38875\n57184,61178\n13551,43809\n4722,42851\n26655,43708\n53147,61193\n31755,49595\n27332,63687\n56633,64748\n25486,29802\n16167,51408\n57858,59881\n36588,39248\n9756,58964\n13479,29517\n49977,58960\n18674,57835\n43534,44121\n8048,26694\n18041,46968\n18572,58734\n25561,54171\n32631,45535\n39953,54682\n21367,37666\n29912,46780\n47242,57961\n1219,44222\n28693,28738\n7670,53391\n16137,21290\n22460,24621\n35819,36826\n25941,26092\n33775,43092\n47340,62320\n26220,54518\n3962,8804\n5521,10741\n19774,21248\n765,35383\n6250,9602\n41363,56317\n37411,63847\n36277,65330\n21671,33781\n28181,32231\n4655,58179\n4235,28586\n25856,54174\n40205,53651\n58081,61022\n12503,16946\n38887,58843\n33020,40161\n34858,53702\n13243,29746\n28043,63312\n40636,45573\n22734,47197\n33882,36484\n10256,36835\n27948,45535\n47932,54806\n23595,29767\n25322,50827\n28199,48121\n45466,61946\n46241,51396\n7165,25554\n30106,38726\n3299,14987\n1025,54670\n5116,56605\n32782,43847\n14286,37461\n8007,43986\n3668,39295\n3692,9994\n1285,28520\n40461,54541\n34087,56615\n6402,59511\n7079,33882\n9382,36659\n300,50246\n55621,60085\n33104,64682\n6602,17788\n33282,38379\n5437,49997\n33881,43359\n11209,61510\n28877,38698\n42986,61730\n29619,55031\n34906,36300\n24064,44569\n15682,37940\n26658,56813\n7380,29088\n19673,26305\n26452,54616\n23622,25570\n9191,17048\n5689,28141\n16861,19500\n7812,27843\n32445,46295\n15963,51451\n4855,25813\n222,14462\n85,12398\n9337,17568\n9086,11461\n39249,59052\n14495,61564\n24642,31632\n17358,34326\n4479,28595\n1710,11393\n28220,62566\n49966,63017\n481,44567\n42279,46265\n14294,36196\n14944,50618\n24305,58388\n26374,56124\n20842,39888\n59579,59651\n21795,38771\n28993,55907\n2174,59268\n34893,46957\n11750,22055\n12302,21098\n26467,35951\n3194,36224\n15827,18010\n10103,15935\n61320,62680\n47728,53656\n11522,23617\n78,12372\n20737,45850\n35590,40977\n18837,28773\n32877,49909\n37752,49965\n42306,48999\n8075,33056\n6039,22758\n9689,12987\n28279,44058\n16828,45509\n639,34884\n18310,62013\n25735,59405\n1628,44003\n54865,63669\n3731,29512\n38173,45000\n42601,47149\n24500,47639\n7610,38115\n3778,64210\n18666,38992\n12175,63057\n12651,22390\n25603,61139\n9958,17905\n24558,26025\n8592,28511\n5997,40280\n719,37569\n15425,51756\n29361,37314\n4024,32525\n14501,42095\n54858,63641\n33161,55468\n18276,48931\n15527,62741\n39339,40649\n5921,11311\n16729,56268\n45851,59708\n15066,47526\n37752,53126\n26466,64779\n5898,21033\n3971,33516\n36474,47559\n34692,54394\n51710,56280\n20829,29636\n7200,49298\n7192,17687\n34383,63744\n20438,46558\n19101,55729\n41896,59746\n5810,16736\n38320,54936\n3999,62136\n18685,38315\n12516,54247\n25783,29002\n44360,54171\n13605,20847\n8769,44005\n11126,37421\n9676,31200\n36187,59514\n2158,3530\n29581,52761\n25077,51750\n23874,34888\n46639,57113\n21541,42606\n4978,63887\n27124,38471\n17290,20259\n49727,65223\n12227,51193\n12832,65036\n27802,52577\n32660,47767\n23364,53018\n4800,38731\n12698,59650\n28336,49097\n2568,62019\n36623,50048\n12048,32634\n33938,65159\n56024,63073\n26444,44495\n31423,61275\n15190,25971\n36606,46208\n49587,65240\n35066,63074\n7577,63169\n3371,19946\n34797,38024\n35426,59062\n28310,35564\n1092,46689\n9339,52489\n16832,31357\n24189,43370\n11115,18270\n12784,12990\n21420,57694\n26474,30874\n11746,63500\n26703,55345\n24681,40326\n51436,57487\n30471,53345\n28131,58881\n21910,39102\n46011,59113\n24737,48166\n35574,62529\n20389,35396\n4668,11926\n6094,30375\n611,59968\n93,42123\n1991,13662\n8340,29817\n566,47650\n23207,51148\n28610,28617\n3225,3252\n5970,38931\n15799,55140\n56179,63152\n14438,32041\n14825,35179\n22778,49946\n30227,41899\n10588,60119\n37863,46900\n28397,57950\n11729,25759\n40895,41506\n21492,52623\n64290,65502\n8325,15220\n1548,39809\n7262,26703\n35922,57939\n13654,24183\n28898,62666\n8128,30852\n2843,56158\n45327,64159\n8767,18564\n44911,59602\n40034,58982\n12677,49442\n22692,27435\n10205,53674\n40955,48403\n4405,52764\n32609,62311\n48655,49489\n10256,54469\n29970,40508\n54729,61092\n38176,58683\n38191,56029\n2284,58027\n2384,26721\n10147,48900\n9431,39712\n16677,21244\n26332,26632\n25309,39964\n10937,12326\n6201,21180\n15343,50437\n45405,48521\n54007,54263\n19226,20275\n21319,34277\n53104,54516\n17233,36503\n15312,44602\n25203,29912\n35298,64993\n49494,64256\n15386,34640\n3370,49603\n39451,56176\n60915,64768\n22,26838\n13614,35447\n14371,21352\n6288,48991\n35606,49055\n22160,54548\n36213,62754\n23364,58513\n22720,35809\n40816,52208\n8227,48882\n1648,53897\n0,27161\n47934,65536";

        //2^32 bytes
		String params = "4294967296\n15\n10\n1000\n1140696597,2186359770\n178388132,2106447153\n1625502788,1737992065\n716776888,3648582048\n2228092719,2803644682\n348938331,3096636155\n146117119,3984380730\n972817455,2146817621\n611100957,1082998415\n2309413356,3900477907\n2247353115,3687232482\n1023939712,3648829320\n497451893,2779560797\n461990758,1226396279\n422685079,1229607649\n2206895007,3918705052\n1137443612,1470855022\n2110613196,3400980583\n1102946301,2657129829\n58099610,4277870591\n1970370846,2237213536\n649665601,3768041826\n795343249,3993699203\n685583872,3367940612\n280850177,4023234593\n1634307002,2972278210\n119719606,3811157207\n1883315646,3936612325\n980089455,2616268527\n2729916554,3974383262\n235437644,4261376308\n1031183122,1269661973\n1121470019,1657294056\n965064513,1581164317\n748640696,3299056325\n1352589675,2506936150\n3857133920,3883996488\n303372557,1867395237\n2770140327,3600569947\n2663401233,3881500664\n25325688,1825344777\n2279413845,4166491802\n1463507499,2542113287\n1947621571,3828054257\n2981650853,3929365455\n478565612,959640870\n2096742803,3428585899\n728329366,1894406908\n123462819,3792890513\n1541048595,3711834706\n2817509251,3157707417\n422811705,2130793908\n1129187378,1993361174\n2198482663,2561872845\n1776729320,2021285509\n673447981,2204112930\n840360689,1040933920\n2351624341,3006411240\n435470834,1492596021\n496655083,3795977230\n3070853359,3735385628\n720227511,3791686873\n331815163,3019681179\n2875517710,2955316330\n259131269,4245831768\n2090638709,2669829777\n627085468,1308787024\n4103865419,4125263682\n3221595226,3753093411\n1631615815,2653688304\n250347520,3051149812\n2340970205,2426862261\n508032978,4182996784\n633290222,2725385676\n148166565,920637498\n1106670813,3324739788\n2618672456,3374553304\n127048068,3907248301\n1606373883,1777974426\n1960231142,3971012929\n848227589,4156334037\n3373196381,3600347779\n1659964912,3314564135\n2320880573,4072975596\n1274857451,2349495022\n507833492,3217417654\n710104425,4258249795\n289221549,3257289161\n607761190,1858177073\n215044083,4148218592\n980789101,1662552005\n635648359,2496362058\n442552474,1858853096\n22061776,1693795934\n1258239208,3783857208\n2952782064,3170424014\n3096100310,3880864687\n2765406394,2909310129\n3143586625,3859610495\n1894418521,4010496483\n1508074282,3998044657\n294807137,4097264197\n59113925,1467863586\n1701275191,2598810788\n627514661,1744694847\n324389418,2644507633\n365625075,1427807266\n3207209876,3226179238\n994019997,1351247453\n181712191,695133031\n2378521040,2884643000\n3859831515,4241506508\n379817362,638011017\n1514410811,3653551638\n700486182,3020478759\n849924125,4026728262\n175045226,1126931201\n1660932822,2786675403\n297288422,3025992872\n1094938130,3998758124\n1084134184,2888356481\n1401614219,4056382182\n1123012250,4023350160\n2794401989,3617536024\n1280573693,2725314253\n2805590055,4205725558\n1519965196,3012064348\n1267452689,3179479503\n2019242207,3257609562\n2322350529,3470647566\n647595105,862469061\n1276545899,3282737469\n2136043533,2706378261\n831551375,1822087416\n293424103,466594903\n1669658873,1883047599\n1081811749,2757347105\n2672124627,2780415106\n129876829,1068568372\n3062750913,3150271328\n2314633448,3815802861\n822797005,3317828022\n2655816680,4030809515\n1521695277,2112656979\n1379691332,3497135905\n1624943172,4192658235\n90351626,398336833\n416173401,2273097583\n3107696669,3929176917\n1163451578,1967160909\n246498117,3570676407\n2385123398,2554601288\n1318566960,1571799219\n2650749190,4267953381\n2580673104,3507721622\n232897912,255554702\n1865330753,2951257760\n1542807552,2891968762\n743211858,2110210641\n480765482,2466706334\n1535219902,1595694100\n409749521,4230446877\n1817484859,3288089774\n3200947013,3720098554\n2240324634,3476308177\n1100241815,3792677047\n89242321,3641730377\n257860366,2960441809\n89886896,1119740705\n170069412,2541472540\n441028486,1551286193\n34073366,1995739152\n501461229,3086076617\n2401758442,2867168195\n2004157459,2436230474\n150525602,1592494467\n3714480827,4126157934\n3243459557,3908292371\n1227546043,1416874482\n2022191794,2444788586\n1022433552,2588726387\n1582188764,2475615064\n1166581217,4232701256\n1679440649,2782796273\n497658556,1324456777\n177816195,1633122390\n3108272104,3337003728\n431560958,3951017945\n491679353,2951299415\n3207930744,3259195110\n2576410501,3512076180\n2438985040,3542054348\n205950816,4148173440\n695202659,3567413893\n44899642,2824492095\n1587043909,3663908611\n1662490914,3099838812\n2027716551,3998563261\n152375983,2252671784\n952288261,3471038055\n475426132,1892624446\n1977344578,2905582617\n87708049,2965510983\n934997964,975827541\n344009127,2321610958\n319540791,2551782193\n782565434,1772135830\n720584217,3789880957\n1849710954,3910416618\n561585419,2177264301\n2574000651,3418020405\n2004939112,2922269042\n1242094314,3058153001\n122514334,2142526334\n838840542,4139590848\n2631929074,3277477137\n1659138067,3914424434\n2301461774,3219486016\n1465156488,1751793515\n3207902892,3457651185\n1692189584,3328989977\n1693322088,2180775345\n1644175413,3794300969\n3137005373,3291013624\n1751465325,2100532661\n1063550115,3754684623\n72885827,418526986\n3472072823,4215861771\n1844693497,2127968231\n938452295,2989728112\n327533504,1978636434\n1034966284,1650045559\n3266078014,4144869811\n407699933,3349246179\n40744658,810194721\n606990574,1761268938\n2288183813,2395559758\n2978300875,3763974653\n84330058,2948899204\n4233813239,4236685947\n1494693791,1958853851\n2086796052,4221583059\n1353319555,1638035559\n77354370,2163082033\n545426449,1310714782\n9843271,1099762894\n2842404508,3318776102\n1709252377,2151998425\n571541751,2630886877\n1216995557,3596607044\n235187241,3719146309\n2118211472,3565460258\n3441268742,3944863577\n2454479727,4203832195\n273831967,371230661\n1927203694,4195319006\n3840431352,3913526555\n3341744272,4129243427\n2916617038,4072062718\n590971809,3067292139\n355516481,482006408\n1585768167,4146427669\n1556451900,3996010609\n412242567,523687783\n2098573140,3962143187\n579118864,620112063\n1431576334,1954176121\n36042955,1753805182\n776876072,3751019912\n2357418301,3699328465\n228157734,2194894810\n626188694,1884017884\n1366386417,1955329062\n3098882997,3975061030\n2734012911,4062184357\n2241659064,3136734311\n2779884730,3161123809\n3724148752,3740955700\n982964903,1292990839\n826045795,1760529229\n642712079,835588022\n3673069662,3997235377\n679871696,3348218726\n831096593,3131742818\n1967886853,3673944117\n565893196,2636543925\n2741956123,3229938401\n1102400731,2356463262\n4028844961,4080508245\n2600668735,3481363596\n1168806924,1353365080\n1841442045,3209465407\n1997085472,4225131769\n1382386638,3644950509\n767067493,3673162598\n450992918,3695306842\n597483444,3266435314\n2607331968,3404669004\n137657916,909638287\n168822362,3450741230\n1457359225,3182569458\n832346925,1787096645\n582208311,4067578794\n2015946926,3656472000\n1060098373,1852830271\n3984230551,4270086449\n184778060,2352422470\n6417057,3112826436\n1599158254,2657592763\n236829204,1263403533\n3009438693,3835264687\n2514794935,3118253124\n1549400279,4089702651\n391925804,2091291974\n711178521,1105200391\n7491011,3278137119\n67328131,3280479327\n345493932,531496639\n335323314,2539378160\n2117156647,2130853305\n2783993145,3610964346\n73658020,3625206416\n1241706990,3315270182\n3009198644,3921487991\n1150937274,4058302073\n662973623,1591699190\n112792515,956978184\n777731628,1047323015\n2812921131,4282591128\n506092395,3542348051\n3068237452,3141436051\n1636675958,2393232900\n366699368,1491878898\n380412093,911254573\n299022756,3262175915\n1214581617,2515893253\n2240507960,4184458340\n1724348101,2938679589\n449991355,1781923492\n993644143,2352350811\n25896875,2324160940\n1625304089,3779129757\n655996832,3549772416\n2578202041,3600727394\n1384253410,1743571502\n1780457849,2482890210\n447411559,552467445\n616738484,1888734760\n2026562796,2949328585\n1329182533,1543334502\n176765401,4069565360\n320474588,4078945725\n367092237,998408522\n3128901675,3238401566\n2589568571,3844732491\n1377603481,2068884364\n1964804193,1989583723\n2532406720,3154812853\n3555798445,4292038220\n1200595180,3572449139\n31973581,2268632958\n3251314699,3653309453\n966746589,1931885747\n397462148,3811620521\n982009097,2131913339\n475610255,1933785261\n136877748,1003708099\n1478363041,2110593323\n997448878,2537245558\n2437325489,3822868084\n720937183,3566301277\n1348558461,2476352430\n522568462,2870479229\n764301483,775466752\n246863819,3479814932\n2505067033,3024432445\n1243317124,3883076120\n1760674622,3693284032\n2647279274,4286328515\n166589525,4123783796\n477047351,774333046\n799995391,3768860067\n2642136598,3254501314\n844342239,2340410876\n739828236,3266925755\n3571361252,3700368802\n3097584754,3217155233\n2246503006,3917923202\n475613680,1206423189\n505989003,1522276913\n1545691786,2602451993\n2470160365,2660160949\n2393278279,3096067660\n1484020275,1930645235\n258775669,1907152930\n948675876,3112817510\n1120868456,3190643083\n16875112,3374543364\n2613486842,4239180418\n1543707268,4141747151\n1868890916,3750462027\n658316604,2797592580\n689376550,1372383210\n639259331,692243596\n1082748541,2072396228\n1838626706,3843842295\n1235096718,1288915351\n2425850670,3296341553\n1572257526,3893081041\n1313927300,2593409391\n1049828616,2101163784\n2282031223,3746972421\n1738757288,3142420852\n3022848052,3100994227\n346087824,1277182466\n2011623208,2659241622\n1439827459,4092415500\n1996303813,2978947911\n1088391625,2049249441\n1927430205,2627928984\n369881090,4178409699\n385257042,853257775\n697455155,2332301963\n20504592,2344773097\n1688397118,2708657209\n357579537,2437321215\n369687949,2151431559\n743151341,1082305174\n140437244,3764501062\n248031607,2008279905\n1536405375,2608540880\n1916764864,3419766432\n1301313000,3495094002\n1060229774,1672183211\n705684334,3226389383\n260111488,392289023\n2506840360,4036891953\n1633367144,3684989834\n1254467597,1547765718\n367755614,2772803927\n2338177288,4008046527\n2877516179,3973323214\n2468169208,3920435884\n1235727293,3983773425\n3498540757,4040251234\n2255719592,3520085473\n2991972891,3755939440\n2438295301,2913816242\n123765907,3261696585\n753423740,2404663325\n1305715471,2285672739\n3006778020,4089106535\n1112769514,2339812763\n90570858,3253105187\n33791178,1140285878\n595008989,2362459360\n609217309,1915759182\n2362729265,3760823676\n2279764524,2510623597\n2178318591,3691937303\n3162257405,3682878474\n490826397,1860827975\n1223084943,3730477499\n287569743,2625960249\n1158244280,1478901710\n567717782,2448692235\n579704167,4015929206\n848767299,1441543472\n307410434,3916068434\n1543269184,4110032955\n412705858,3837709023\n679165213,1647741102\n164962534,2756872235\n287116124,2314926899\n716027088,3943876418\n58817817,535194459\n3104205582,3276040765\n1073557401,3669788429\n208783979,2640976640\n484449036,1495281831\n341587274,2027006576\n2256091682,2929293803\n2215630060,4038944472\n565764696,2795084539\n1485644383,3638640480\n49307973,292694820\n29163093,1256169438\n934852448,1296659139\n2421042684,2557211919\n936284714,1677546565\n1452227881,3203498487\n1584817731,1817717866\n2454295989,2525957176\n123462207,391401242\n2151869929,3943866281\n1063502797,2804346916\n70168955,280045715\n2233766973,2801814750\n1634817957,1692880671\n1587442516,3234668771\n1027147319,2628090031\n1640188899,3973962048\n1416375550,1603976727\n2001955880,3881723654\n2182704598,3413335039\n580251456,880450065\n8122235,816800517\n42989483,3684115761\n37299859,3609856682\n3307310332,3681473153\n64738449,4009146234\n815374681,2981153627\n2470725766,4197460856\n2415361549,4044889836\n1422310636,2947874795\n1460531383,4013094913\n166484175,1320684652\n2438596844,2795376594\n3172654730,4107591662\n1314501634,3260326311\n1208290907,3873671712\n1252193545,2880154062\n1389357008,3078511313\n791256878,1891193517\n917005280,1565586907\n2291565210,3574578669\n2030638972,2715951824\n846113143,4284441484\n10039839,2642748813\n1778398812,3541065495\n2199078417,3849650187\n2610778202,4130406061\n2860852695,3957617363\n1927104146,2719994419\n908449671,2406656207\n444998303,1255542939\n776837943,2336535154\n998996087,1185734410\n2357154431,2392506221\n2821444,2665081067\n806524646,2045561140\n77982983,2039713940\n328954519,1076711501\n648598779,2496155996\n1761840996,3835559516\n2331116487,3669765010\n641645711,1977653085\n2099827515,2444354886\n1466559401,1794964529\n759230440,3585338789\n796471967,2902516479\n2624795594,3028602705\n121629166,2237116705\n1112914023,2182771093\n3514278193,4180038641\n3688352447,3850941898\n1286362369,3779679006\n825269861,1096132678\n801520074,3698429406\n1660458390,1909537987\n886877629,3573438392\n1973550084,3856034020\n702012231,1154271798\n2236393141,3317294965\n190804678,1663831250\n2246369336,4238478537\n3623591899,3830497251\n2224124264,2944582793\n1632693537,1898187256\n136767988,927206596\n366376862,3422558661\n213862911,1725741655\n1359307865,3741860758\n1828514860,4000357885\n2064345994,3720108620\n1076035434,3901300813\n1051541764,2261919664\n2257965114,3601070089\n1104371511,1502396016\n725110557,2573694272\n3483183124,3646080576\n1009330351,3628270019\n2183068751,2874856720\n389731019,2332161620\n1279863054,1640004080\n1831913809,2293025418\n122058954,996587503\n111790434,1178114327\n1417329929,2982771442\n3223325838,3417300233\n39616536,230819392\n2853541954,3127270932\n2513958665,3947237245\n718707895,4174553524\n164276205,943204789\n983381753,3039830706\n1514500191,3047227093\n467103878,2528415074\n2778084957,3140114074\n97270614,3708189334\n649404424,2255691414\n973087258,3614917078\n1285871818,1846977890\n1480700057,2478921571\n1230804017,3680835015\n1893881181,3291624903\n1025654965,1645034620\n841098116,1691840371\n1892798106,2656010759\n995811085,3742777191\n1374169863,3141425104\n1970770329,3486554082\n1236027524,2903264560\n3137558724,3153457249\n899646633,3040532586\n735120410,2292399484\n4349842,3044539298\n820266082,3161154930\n846663908,4275831081\n1000256788,1439633331\n597130220,1445334512\n1979951429,2431579088\n504223461,3829157489\n456040017,1683156330\n2992489022,3816194255\n198444399,1254893115\n3268339796,4096318380\n122081494,239532628\n1591673136,3981739496\n68005390,2163101193\n4004791890,4076585705\n230668380,3927971443\n822658125,2163604859\n2657874538,3759482626\n1803361853,3996175176\n2277873927,3626659505\n2787210529,3266344281\n3000376201,3991269720\n980578825,3021112007\n1022341509,2320725360\n1620324716,2718209218\n3336889406,4241704046\n3078983514,3619026209\n1124912565,1981196914\n1558737028,2649970640\n1329076193,2717239111\n3346007033,3814094907\n920820022,2915587215\n845000532,1763547339\n3294160744,3378848389\n687456851,2575243446\n805782806,3689955539\n2014277521,2213206945\n22659628,3253872301\n1897963982,2206259979\n1504634966,4185244878\n849115031,2055541100\n2404435844,3817767584\n1069954899,1138015728\n2419699961,3406579566\n90959338,3165027915\n1722875155,3049709046\n2753507748,2782249510\n2214779925,4117696433\n55173744,3558783838\n2613001784,2690947886\n2298065895,3032227150\n262595158,2386377050\n287325159,3229768027\n151991717,4117249744\n1605516007,4281171142\n357996793,3258725494\n2202201508,3723463223\n295724742,2831824502\n169030334,3973452451\n3004903230,4255114109\n723097018,3702190687\n2965696525,3644196684\n536425890,985770128\n219826011,2746701822\n335916946,3310148733\n404002474,2392858588\n1804350657,3128714797\n2881267626,3464652944\n2869967276,3821589867\n3560341089,4157565456\n2496011168,3219385536\n922692985,4090847133\n884724667,4232133969\n1023023642,2218123758\n914090274,1658814947\n1281591268,2955154409\n444754059,2674145538\n1228570641,2582808671\n3503341076,3730128894\n1610258844,2074350752\n940557399,3340726181\n666463744,1060439556\n2322542519,4196849505\n2854634728,2890840422\n286202338,2635530848\n1507563891,1979606518\n2599407861,2629201716\n1306614468,3603836768\n23707279,742439004\n931966782,2297348946\n711861044,1470894170\n1223518106,2627801996\n1298459928,2150568620\n60822978,2270707258\n507211605,597634773\n888924681,3273131166\n1003970030,2162575289\n2756562402,2843831484\n3649305365,3753368015\n3472755245,4066602857\n329456628,1288536007\n451966273,3018954427\n1501543006,1944339090\n245991848,3458952011\n808642945,1200640990\n1746720853,3234335055\n1367466291,1883669949\n1504777589,1979141521\n1770403172,4067857274\n1977557454,4071157326\n1446237545,1589927936\n2097818401,3657293810\n292685797,2260166324\n1412206632,1469326416\n3139678716,3578394771\n679249477,3039800051\n2404312259,4195411750\n1697775030,2027877224\n362306674,1187014537\n1576135062,3423827709\n50788190,986511452\n157542808,2950957268\n2838894113,3377347327\n2223302245,3243123350\n764215778,4253452472\n1877014777,2708346215\n2893154237,3940626962\n274109992,3898121964\n2066269385,2736886904\n1234841984,3098991706\n2168475472,2908962891\n2934878823,3446227790\n496041461,4113026821\n1367051586,1441187920\n630438320,2870531698\n1194018297,2832169037\n1220178140,2328846815\n2467796955,4192728170\n3123287894,3776606049\n2241118416,2843626830\n1094211442,2893432316\n2029504138,3689811423\n268419149,3919266214\n2094054664,3930187972\n2080060552,2103747698\n391222066,1177388600\n539259556,2860190137\n467315996,1902223262\n2375598453,4134068917\n325943659,2454618393\n83673723,1556805460\n1992463558,4105556361\n614072359,4198625260\n1171602790,4169883338\n2602123345,3315351312\n2914540106,3618008271\n3966437505,4186422608\n113295770,249436788\n2748640069,3048428721\n2009776416,2393010300\n405205461,3521463848\n1609472462,3918000688\n2546502586,3605165924\n412030120,1023581188\n100062707,661567602\n510932719,3781487236\n3693407274,3717247190\n50202703,3845961514\n782309863,3419218334\n138750961,700708330\n2354396799,3300237606\n1499245379,3347978602\n1420017570,3041925093\n1970936817,2645959477\n941111141,1855675203\n2746061165,3569691262\n772116054,3043954724\n2490670276,3341036419\n1076416012,3260074253\n1476470013,3138466419\n1233875293,1906508589\n3607050849,4224770374\n758276630,2639485937\n275288908,2945044944\n987561929,2854055465\n3156998575,3625917449\n1048987005,3863332627\n195980550,3795136218\n2116971407,2492941991\n2712930676,4075705300\n522206297,2029505551\n1201348328,3303746236\n77672336,3224653025\n3644091076,3875549188\n174368187,2903565454\n2127808658,2948841639\n158357832,1076203663\n716441694,1359120666\n1974078861,3942094000\n210519296,1210873637\n3500975977,3835889625\n97235727,3394208537\n340283292,3955105254\n182393288,2208213789\n3127222789,3395718677\n409738963,3390814871\n1674240239,3037226073\n546943480,3891822940\n1977152797,2264662714\n222400983,2637324199\n92483193,478804400\n2384994534,2598756513\n3367440719,4100646389\n834577592,2004362307\n1786528404,3913235279\n1105350901,1279954743\n2983007871,3573001222\n26536306,2736547556\n2840345528,3500452578\n1438534168,4119556767\n2299295882,2433699409\n497626736,2185776869\n2715583659,3500283348\n558741550,2290260226\n334981756,4084869360\n1875747221,3357209184\n702828069,2975714555\n1866938341,3521458556\n2381179105,3385061081\n1150613399,2273251278\n777123349,2788347215\n668497371,850814839\n1076167812,4234227488\n1600410991,2058661252\n1307465704,3055549810\n2022403137,2691664261\n894552408,1681944807\n734754893,2493372454\n395327940,1188693158\n1119424164,3595740591\n1565636565,3233942346\n1472024842,2575449657\n1109049673,1874666293\n152832016,1855740309\n338136422,1457954207\n895236729,3192045491\n294264442,2139810941\n1111710355,1675720704\n3243452987,3775399779\n912654549,3731304884\n96269673,1913308103\n1721050809,3335256408\n2677178206,4181375683\n1627883575,2992914391\n1904752311,1942653389\n720415197,1046934651\n54439490,2767756159\n46674045,1776047160\n1389302231,1480110032\n1048738178,1760307270\n206595224,4072461853\n159282165,2293425704\n372249976,2249379995\n995315960,1647061231\n1824155784,3322662560\n3663465479,4061953754\n507807541,806247584\n468678958,1951929772\n868327722,1216851488\n1460967309,2621543458\n2820383284,4207670664\n1819817371,3587856979\n813767987,1680335567\n2414231808,4109593681\n189915713,277171205\n2980570321,3901272269\n3317293453,3669147393\n1643474569,4114788756\n1120155560,1808487554\n41111266,2136689908\n1416949467,3774282807\n833266759,1211591990\n2754734108,4080527640\n1198692262,1319540731\n1901963993,3829402093\n1397312255,1608644349\n2823634417,3330663385\n25815637,376116114\n1072682273,1123620425\n483016628,2125079944\n2023909953,2302613183\n266679690,4012584422\n362100584,3444467474\n1674149439,2187541329\n509887725,2814245080\n1120540737,2235302102\n146272485,4134576373\n2245208830,4004366826\n2694857900,3631743462\n232666898,3760001873\n306100020,839258439\n2326321157,2346311148\n899262077,1124726899\n1744174456,3161955889\n2214011988,3051914053\n669820158,4258332175\n2037480500,2058495630\n1820698937,2093112357\n349443924,4270398315\n59301175,4237151266\n3368463555,4080994290\n318936732,1793533491\n1542911230,4237832922\n66293928,4033168417\n2032642087,3857574274\n617037265,1055381989\n564641288,3019705134\n444426304,1417540924\n950547710,3651444437\n797450429,3384936021\n1115106247,1695846228\n2571689256,2868262600\n328307437,1686555588\n994551464,4063768712\n1248655362,1543679526\n412078076,2770008638\n1144344555,2130810063\n3147649478,4077210248\n3558181650,4147729908\n815531205,3369282353\n538464027,1974501817\n289588304,1645467353\n3910592093,4189341151\n1056392701,2998219891\n386144653,2900983691\n2855206933,4159069607\n355477996,486137354\n153998787,2456607011\n3300027264,3606134390\n2598910729,4293333579\n72628581,1577271117\n1011230344,2917448456\n714174943,1842578082\n2331062351,2528067011\n2028126795,3485824058\n3708644036,3944798112\n2241273142,2271768849\n2918796134,2927812473\n812013933,2028308664\n2649804224,3976822976\n713605308,3711348256\n1177948709,2465194955\n536030583,2223244490\n347218864,1590294551\n113147617,2895214270\n2799413057,3833999028\n1933675778,3723464005\n1924843554,3223314341\n2605547901,3007518920\n1002748258,1950269547\n204977712,2625647255\n73400765,2681432914\n834785259,2172493607\n2060719964,3051776670\n2964947009,3118360903\n1411468109,1894233478\n2349792216,3686727261\n2688647651,3927218393\n2221404631,2308464030\n593700243,691502909\n2557463049,2606873057\n273183535,2767284538\n272522039,2593537629\n1753441311,1884653428\n341077140,1750901614\n484905033,4282547640\n1999068040,3031779345\n517507863,3224523184\n3722600734,4145727464\n745864317,3778556985\n1235797013,3656319121\n1986680727,4282756465\n0,1207285301\n1538365454,4294967296";

        Scanner input = new Scanner(params);
		input.useDelimiter("[,\\s]");
		long numBytes = 0, numChunks = 0;
		
		//Get number of bytes in target image
		if(input.hasNextLong()) numBytes = input.nextLong();

		//Set latency and bandwidth for cost calculation
		if(input.hasNextLong()) Chunk.latency = input.nextLong();
		if(input.hasNextLong()) Chunk.bandwidth = input.nextLong();
		
		//Get number of chunks that will be provided
		if(input.hasNextLong()) numChunks = input.nextLong();
		
		long min= Long.MAX_VALUE, max = Long.MIN_VALUE;
		
		Set<Chunk> chunks = new HashSet<>();
		while(input.hasNextLine()){
			long left = 0, right = 0;
			if(input.hasNextLong()) left = input.nextLong();
			if(input.hasNextLong()) right = input.nextLong();
			min = Math.min(min, left);
			max = Math.max(max, right);
			//check that chunk contains information
			if(right > left){
				chunks.add(new Chunk(left,right));
			}
		}
		input.close();
		
		if(Chunk.bandwidth <= 0){
			System.err.println("insufficient bandwidth");
			return;
		}
		
		if(Chunk.latency < 0){
			System.err.println("impossible latency");
			return;
		}
		
		if(numChunks != chunks.size()){
			System.err.println("Wrong number of chunks provided");
			return;
		}
		
		if(min > 0 || max < numBytes){
			System.err.println("min/max doesn't cover numBytes");			
			return;
		}
		
		//Create processor for problem
		ProblemProcessorParallel processor = new ProblemProcessorParallel(numBytes, chunks);
		if(processor.getBestSequence() != null){
			for(Chunk chunk : processor.getBestSequence()){
				System.err.println(chunk);
			}
		}
		
		//Output solution if one was found
		if(processor.getLowestCost() != null){
			System.out.println(String.format("%.3f", processor.getLowestCost()));
		}
			
	}	
}
