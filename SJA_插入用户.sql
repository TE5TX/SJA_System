-- ============================================
-- SJA 用户导入脚本：根据两份 Excel 花名册生成
-- 主花名册：C:\Users\Administrator\Desktop\稿费整理工具\第二十届校记者团花名册.xlsx
-- 二轮花名册：C:\Users\Administrator\Desktop\稿费整理工具\第二十届校学生记者团二轮花名册（收集结果）.xlsx
-- 适配数据库结构：SJA_System_db.sql
-- 登录规则：用户名=学号；普通成员初始密码=asdf123456；管理员初始密码=asdfadmin
-- 统计：主花名册 99 人，二轮花名册 60 人，二轮重复学号覆盖 5 人，导入账号总数 155 个。
-- ============================================

USE `sja_system_db`;

-- 以下是 BCrypt 加密后的密码，不是明文密码。
-- asdf123456 -> $2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm
-- asdfadmin  -> $2b$10$uWpPds.1IbfpsD2.4rhsp./r5.1i0VxcJarfLlaWpQBgB7zxgmEqO
SET @pwd_user = '$2b$10$Ih0c9.50/XmozWaBZWP9ree0VQn9ov5i7pb5GddchwYog5Q1hwhSm';
SET @pwd_admin = '$2b$10$uWpPds.1IbfpsD2.4rhsp./r5.1i0VxcJarfLlaWpQBgB7zxgmEqO';

-- 0. 补充导入需要用到的角色。
-- SJA 初始库已有「实习部员（二轮）」(role_code=9)，这里只额外补充「管理员」和「正式部员」。
INSERT INTO `sys_role` (`role_name`, `role_code`, `sort_order`, `is_valid`, `remark`) VALUES
('管理员', 0, 0, 1, 'system administrator'),
('正式部员', 14, 35, 1, 'formal member from roster')
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`), `sort_order` = VALUES(`sort_order`), `is_valid` = 1, `remark` = VALUES(`remark`);

-- 管理员授予全部菜单/按钮权限。
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`)
SELECT r.id, m.id, '管理员-全部权限'
FROM `sys_role` r
JOIN `sys_menu` m ON m.is_valid = 1
WHERE r.role_code = 0
ON DUPLICATE KEY UPDATE `remark` = VALUES(`remark`);

-- 正式部员复用留任部员(role_code=10)的基础权限。
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `remark`)
SELECT formal_role.id, rm.menu_id, '正式部员-基础权限'
FROM `sys_role` formal_role
JOIN `sys_role` retained_role ON retained_role.role_code = 10
JOIN `sys_role_menu` rm ON rm.role_id = retained_role.id
WHERE formal_role.role_code = 14
ON DUPLICATE KEY UPDATE `remark` = VALUES(`remark`);

-- 本脚本不删除旧的用户-角色/用户-部门关联：你会先重建整个数据库再导入数据。
-- 如果不清库重复执行，ON DUPLICATE KEY 只会避免相同关联报错，不会清理用户之前多余的关联。

-- 以下行没有导入：Excel 学号为空，sys_user.student_no 不能为空。
--   Excel row 95: name=郭华杰 dept=视频部 status=正式部员
--   Excel row 96: name=谭诗雯 dept=视频部 status=正式部员

-- 1. 插入用户基础信息。
-- 二轮花名册人员统一设为「实习部员（二轮）」，role_code=9。
-- 两份花名册学号重复时，以二轮花名册为准。
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `gender`, `id_card`, `student_no`, `college`, `class_name`, `dorm_address`, `phone`, `bank_card`, `remark`) VALUES
('admin', @pwd_admin, '系统管理员', 0, NULL, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '管理员'),
('2023030103052', @pwd_user, '黄义鉴', 1, '445322200411300030', '2023030103052', '计算机学院', '23计科F班', '18栋223', '18033372534', NULL, '主席团成员'),
('2024050401003', @pwd_user, '蔡沐哲', 2, '445121200606211620', '2024050401003', '人文社会科学学院', '24新闻B班', '香晖A825', '17817688736', NULL, '正式部员'),
('2023070303020', @pwd_user, '陈美艳', 2, '440803200406053921', '2023070303020', '经贸学院', '23国贸C班', '香晖B1207', '18022686137', NULL, '正式部员'),
('2024070102016', @pwd_user, '郭潇远', 2, '410323200502089628', '2024070102016', '经贸学院', '24金融B班', '香晖C912', '18319424396', NULL, '正式部员'),
('2024070202009', @pwd_user, '韩佩桦', 2, '440801200512240621', '2024070202009', '人文社会科学学院', '24新闻A班', '香晖c703', '18927699239', NULL, '正式部员'),
('2024050301005', @pwd_user, '何嘉雨', 2, '440105200608014546', '2024050301005', '人文社会科学学院', '24新闻D班', '香晖A1128', '13640880032', NULL, '正式部员'),
('2024080405010', @pwd_user, '何秋雨', 2, '441781200602080526', '2024080405010', '人文社会科学学院', '24法学D班', '朝晖1129', '17875567183', NULL, '正式部员'),
('2024050402022', @pwd_user, '胡雅琪', 2, '330683200602065626', '2024050402022', '人文社会科学学院', '24新闻D班', '香晖A1124', '13588511814', NULL, '正式部员'),
('2024050402030', @pwd_user, '赖心敏', 2, '441323200610136029', '2024050402030', '人文社会科学学院', '24新闻C', '香晖A831', '13631149072', NULL, '正式部员'),
('2024060101023', @pwd_user, '李依澄', 2, '442000200604011764', '2024060101023', '管理学院', '24财务管理A班', '香晖B523', '18024233233', NULL, '正式部员'),
('2024050401032', @pwd_user, '梁泳琳', 1, '440703200510286016', '2024050401032', '人文社会科学学院', '24新闻B班', '香晖A821', '19531473628', NULL, '实习部员（二轮）'),
('2024050401034', @pwd_user, '林佳仪', 2, '445202200603120623', '2024050401034', '人文社会科学学院', '24新闻A', '香晖A817', '15820534446', NULL, '正式部员'),
('2024050201020', @pwd_user, '林嘉钰', 2, '440103200608273326', '2024050201020', '人文社会科学学院', '24行管A', '香晖A727', '19502060827', NULL, '正式部员'),
('2023080304017', @pwd_user, '刘诗怡', 2, '441324200409162344', '2023080304017', '外国语学院', '23商英D班', '朝晖426', '15398891461', NULL, '正式部员'),
('2024050402040', @pwd_user, '刘韵', 2, '411525200612037524', '2024050402040', '人文社会科学学院', '24新闻D', '香晖A1124', '17610556632', NULL, '正式部员'),
('2024050401038', @pwd_user, '陆爱珊', 2, '440106200604186929', '2024050401038', '人文社会科学学院', '24新闻A', '香晖A814', '13725325448', NULL, '正式部员'),
('2024050402050', @pwd_user, '韦雅萍', 2, '441781200602144681', '2024050402050', '人文社会科学学院', '24新闻D班', '香晖A1119', '13829878625', NULL, '正式部员'),
('2024050401056', @pwd_user, '幸美琪', 2, '441481200512296300', '2024050401056', '人文社会科学学院', '24新闻B班', '香晖A828', '15819985715', NULL, '正式成员'),
('2024050402054', @pwd_user, '鄢珂悦', 2, '420984200509133326', '2024050402054', '人文社会科学学院', '24新闻D班', '香晖A1120', '13642568597', NULL, '正式部员'),
('2024030101078', @pwd_user, '余梦洁', 2, '361130200604166224', '2024030101078', '计算机学院', '24计科A班', '香晖B402', '15967106959', NULL, '正式部员'),
('2024050402061', @pwd_user, '张丽萍', 2, '362430200512161722', '2024050402061', '人文社会科学学院', '24新闻C班', '香晖A830', '15059850198', NULL, '正式部员'),
('2024050401064', @pwd_user, '张宇桐', 2, '441302200607248722', '2024050401064', '人文社会科学学院', '24新闻A班', '香晖A819', '13794531748', NULL, '正式部员'),
('2023050401068', @pwd_user, '赵梦悠然', 2, '150723200510139224', '2023050401068', '人文社会科学学院', '23新闻B班', '凤翔3-410', '18925387267', NULL, '留任部员'),
('2024050401072', @pwd_user, '朱芷瑶', 2, '440106200510200929', '2024050401072', '人文社会科学学院', '24新闻B班', '香晖A821', '13169316472', NULL, '正式部员'),
('2023050401008', @pwd_user, '陈颖琪', 2, '440105200403137622', '2023050401008', '人文社会科学学院', '23新闻A', '凤翔3栋306', '15113391203', NULL, '留任部员'),
('2024040701012', @pwd_user, '杜嘉俊', 1, '440606200511040055', '2024040701012', '材料与食品学院', '24食安A', '19栋525', '13172331181', NULL, '正式部员'),
('2024050401022', @pwd_user, '黄渲淇', 2, '445121200509073123', '2024050401022', '人文社会科学学院', '24新闻B', '香晖A826', '13715715161', NULL, '正式部员'),
('2023050401074', @pwd_user, '黄莹', 2, '445302200411040624', '2023050401074', '人文社会科学学院', '23新闻B班', '凤翔3-406', '13826897475', NULL, '留任部员'),
('2023050402057', @pwd_user, '孔洁茹', 2, '441802200301062420', '2023050402057', '人文社会科学学院', '23新闻D班', '凤翔3栋610', '15816262304', NULL, '留任部员'),
('2023050402059', @pwd_user, '李敏茹', 2, '441421200502204429', '2023050402059', '人文社会科学学院', '23新闻A', '凤翔3栋606', '13714393933', NULL, '留任部员'),
('2023080102025', @pwd_user, '李欣悦', 2, '430281200508310284', '2023080102025', '外国语学院', '23翻译b班', '朝晖434', '18124126099', NULL, '留任部员'),
('2024090101018', @pwd_user, '李泽翰', 1, '330902200507041018', '2024090101018', '艺术设计学院', '24产设A班', '10栋652', '15068018728', NULL, '正式部员'),
('2023050401083', @pwd_user, '林丽娜', 2, '441581200501142125', '2023050401083', '人文社会科学学院', '23新闻B班', '凤翔3-403', '13631126281', NULL, '实习部员（二轮）'),
('2023050402067', @pwd_user, '陆烁琳', 2, '445121200409225687', '2023050402067', '人文社会科学学院', '23法学F班', '凤翔3栋507', '13609665213', NULL, '留任部员'),
('2023050402028', @pwd_user, '罗艾如', 2, '440605200504300289', '2023050402028', '人文社会科学学院', '23新闻C班', '凤翔3栋502', '13690316769', NULL, '留任部员'),
('2024080405021', @pwd_user, '谭晓茵', 2, '440785200610023120', '2024080405021', '外国语学院', '24英语B', '朝晖1130', '18125495832', NULL, '正式部员'),
('2023010401044', @pwd_user, '王广宇', 1, '320724200512103071', '2023010401044', '电子信息学院', '23光电A班', '17栋148', '18552219838', NULL, '留任部员'),
('2024050103044', @pwd_user, '王熙颖', 2, '511622200606101921', '2024050103044', '人文社会科学学院', '24法学C班', '香晖A707', '13808862619', NULL, '正式部员'),
('2023070103009', @pwd_user, '王源至', 0, NULL, '2023070103009', '经贸学院', '23金融c班', '岐头17栋', '18807611302', NULL, '留任部员'),
('2024050401052', @pwd_user, '吴佩芹', 2, '440803200510022420', '2024050401052', '人文社会科学学院', '24新闻B班', '香晖A825', '18933820977', NULL, '正式部员'),
('2024060101020', @pwd_user, '肖文欣', 2, '441323200511058547', '2024060101020', '管理学院', '24财管A班', '香晖B523', '14776740883', NULL, '正式部员'),
('2023020301072', @pwd_user, '谢晓梅', 2, '441900200410291102', '2023020301072', '机电工程学院', '23自动化A2班', '香晖D1020', '13537341850', NULL, '留任部员'),
('2024050601012', @pwd_user, '徐铭浚', 1, '440111200511177514', '2024050601012', '电子信息学院', '24通信A', '17栋455', '19374982706', NULL, '正式部员'),
('2024030102085', @pwd_user, '詹一方', 1, '440305200510108254', '2024030102085', '计算机学院', '24计科C班', '16栋128', '13824722175', NULL, '正式部员'),
('2024080406024', @pwd_user, '湛楚', 2, '44098120050617002X', '2024080406024', '外国语学院', '24英语C班', '朝晖1136', '18476512388', NULL, '正式部员'),
('202308010125', @pwd_user, '张思琪', 2, '350721200506062126', '202308010125', '外国语学院', '23翻译A班', '朝晖苑428', '17820309053', NULL, '留任部员'),
('2024040802035', @pwd_user, '张镇涛', 1, '440105200607216613', '2024040802035', '材料与食品学院', '24应化C班', '19栋626', '14750051084', NULL, '正式部员'),
('2024020301036', @pwd_user, '郑培琪', 2, '440606200508160128', '2024020301036', '机电工程学院', '24自动化A', '香晖B515', '13360371871', NULL, '正式部员'),
('2024020302072', @pwd_user, '钟昊轩', 1, '445281200501086319', '2024020302072', '机电工程学院', '24自动化B2', '10栋626', '13128369329', NULL, '实习部员（二轮）'),
('2024070301014', @pwd_user, '钟秀萍', 2, '44162120060407382X', '2024070301014', '人文社会科学学院', '24法学A', '香晖c807', '15811968661', NULL, '正式部员'),
('2024060401015', @pwd_user, '周可歆', 2, '440113200604250621', '2024060401015', '管理学院', '24物管A班', '香晖B806', '18820042368', NULL, '正式部员'),
('2024070301016', @pwd_user, '朱颖桐', 2, '440781200510248327', '2024070301016', '人文社会科学学院', '24新闻B班', '香晖C810', '13760512038', NULL, '正式部员'),
('2024080405001', @pwd_user, '蔡丹仪', 2, '445122200601064322', '2024080405001', '外国语学院', '24英语B班', '朝晖1130', '19566039235', NULL, '正式部员'),
('2024090101001', @pwd_user, '蔡嘉宜', 2, '441322200512111323', '2024090101001', '艺术设计学院', '24产设A班', '朝晖620', '13824711226', NULL, '正式部员'),
('2024050402004', @pwd_user, '陈灿群', 2, '445322200409293425', '2024050402004', '人文社会科学学院', '24新闻D班', '香晖A1121', '13178607019', NULL, '正式部员'),
('2024090301003', @pwd_user, '陈李墨', 2, '350304200607251024', '2024090301003', '艺术设计学院', '24环设', '朝晖625', '13705946788', NULL, '正式部员'),
('2024050402010', @pwd_user, '陈颖茵', 2, '440781200607113840', '2024050402010', '人文社会科学学院', '24新闻D', '香晖A1121', '13676152524', NULL, '正式部员'),
('2024010202012', @pwd_user, '郭晓玉', 2, '469027200602100026', '2024010202012', '电信信息学院', '24电信B班', '香晖C118', '17389738393', NULL, '正式部员'),
('2024050402026', @pwd_user, '黄茜', 2, '441900200601205203', '2024050402026', '人文社会科学院', '24新闻D', '香晖A1124', '15899602937', NULL, '正式部员'),
('2024050402038', @pwd_user, '刘佳欣', 2, '441424200406016304', '2024050402038', '人文社会科学学院', '24新闻D班', '香晖A1121', '19128154601', NULL, '正式部员'),
('2024090301019', @pwd_user, '刘婉清', 2, '360921200511051221', '2024090301019', '艺术设计学院', '24环境设计A班', '朝晖620', '15180527186', NULL, '正式部员'),
('2024070102001', @pwd_user, '卢玥彤', 2, '440304200605154628', '2024070102001', '经贸学院', '24金融B班', '香晖C1022', '13622309023', NULL, '正式部员'),
('2024050402017', @pwd_user, '冯心悦', 2, '440781200605255327', '2024050402017', '人文社会科学学院', '24新闻D班', '香晖A1124', '13431730213', NULL, '正式部员'),
('2023090402041', @pwd_user, '许洁娜', 2, '445102200410146346', '2023090402041', '艺术设计学院', '23视传B', '凤翔1-508', '15919505330', NULL, '留任部员'),
('2024070101019', @pwd_user, '钟悦欣', 2, '440604200605180064', '2024070101019', '机电工程学院', '24电气B1', '香晖C902', '13612549937', NULL, '正式部员'),
('2023070204024', @pwd_user, '陈光翻', 1, '44122420050424705X', '2023070204024', '经贸学院', '23电商D', '17-749', '15119830104', NULL, '留任部员'),
('2024010201024', @pwd_user, '陈嘉豪', 1, '440881200510272437', '2024010201024', '电子信息学院', '24电信A班', '10-823', '13924214448', NULL, '正式部员'),
('2023090401005', @pwd_user, '陈梓鸿', 1, '440515200508201314', '2023090401005', '艺术设计学院', '23视传A', '8-217', '15767715658', NULL, '正式部员'),
('2024050401007', @pwd_user, '邓尧', 2, '441202200604290567', '2024050401007', '人文社会科学学院', '24新闻A', '香晖A812', '18998594931', NULL, '正式部员'),
('2024030801006', @pwd_user, '方烁森', 1, '445224200510121211', '2024030801006', '计算机学院', '24人工智能A班', '18-237', '15889653394', NULL, '正式部员'),
('2023090402011', @pwd_user, '郭华杰', 1, '350981200503090153', '2023090402011', '艺术设计学院', '23视传B', '八栋303', '15398885528', NULL, '正式部员'),
('2024020701018', @pwd_user, '李昊阳', 1, '440181200510263911', '2024020701018', '机电工程学院', '24机电', '10-636', '17819775108', NULL, '实习部员（二轮）'),
('2023080401003', @pwd_user, '李佳蓉', 2, '610104200507286142', '2023080401003', '人文社会科学学院', '23新闻b班', '朝晖409', '18681529106', NULL, '正式部员'),
('2024060101034', @pwd_user, '李晴', 1, '430626200603020032', '2024060101034', '管理学院', '24财管A班', '香晖B626', '18928877098', NULL, '正式部员'),
('2024030202094', @pwd_user, '吕淑言', 2, '350722200512220625', '2024030202094', '计算机学院', '24软工D班', '香晖B424', '13159299684', NULL, '正式部员'),
('2024050402046', @pwd_user, '欧紫盈', 2, '441781200502142742', '2024050402046', '人文社会科学学院', '24新闻D班', '香晖A1123', '19128001654', NULL, '正式部员'),
('2024090402028', @pwd_user, '石鹏程', 2, '360622200403210045', '2024090402028', '艺术设计学院', '24视传b班', '朝晖634', '13726326592', NULL, '正式部员'),
('2023050401078', @pwd_user, '王岱妍', 2, '440515200506290026', '2023050401078', '人文社会科学学院', '23新闻B', '凤翔3-402', '18316865007', NULL, '留任部员'),
('2024020102069', @pwd_user, '文涛', 1, '445121200509277337', '2024020102069', '机电工程学院', '24电气B', '十栋541', '13827314124', NULL, '正式部员'),
('2024050402059', @pwd_user, '余全洁', 2, '411502200601112029', '2024050402059', '人文社会科学学院', '24新闻D', '香晖A1120', '18929516067', NULL, '正式部员'),
('2023050402038', @pwd_user, '张伊漫', 2, '445122200409031723', '2023050402038', '人文社会科学学院', '23新闻C班', '3栋503', '15919848618', NULL, '正式部员'),
('2024090501049', @pwd_user, '赵凌梓', 1, '440105200608060913', '2024090501049', '艺术设计学院', '24工设a', '十栋302', '13544331456', NULL, '正式部员'),
('2024050402068', @pwd_user, '钟敏瑜', 2, '440305200512129067', '2024050402068', '人文社会科学学院', '24新闻C', '香晖A829', '13148529039', NULL, '正式部员'),
('2024010202072', @pwd_user, '钟文雄', 1, '350824200602140010', '2024010202072', '电子信息学院', '24 电信 b 班', '10 栋 434', '15728723302', NULL, '正式部员'),
('2024090101043', @pwd_user, '周楚盈', 2, '440605200512100164', '2024090101043', '艺术设计学院', '24产设A', '朝晖619', '13318397784', NULL, '实习部员（二轮）'),
('2024050401071', @pwd_user, '朱榕翎', 2, '469007200409143360', '2024050401071', '人文社会科学学院', '24新闻A', '香晖A812', '13422126669', NULL, '正式部员'),
('2023050401072', @pwd_user, '魏黛琳', 2, '445202200406188020', '2023050401072', '人文社会科学学院', '23新闻B班', '3栋404', '13822097152', NULL, '主席团成员'),
('2023050401063', @pwd_user, '王滢', 2, '441900200507121329', '2023050401063', '人文社会科学学院', '23新闻B班', '凤翔3-409', '13532347206', NULL, '主席团成员'),
('2023060102006', @pwd_user, '龚楚淇', 2, '440103200410274841', '2023060102006', '管理学院', '23财务管理C班', '凤翔4栋205', '13680185067', NULL, '主席团成员'),
('2023080402027', @pwd_user, '何锌颖', 2, '44532220030214522X', '2023080402027', '外国语学院', '23英语B班', '朝晖523', '15818150965', NULL, '主席团成员'),
('2023050402060', @pwd_user, '梁衡', 1, '440803200503312913', '2023050402060', '人文社会科学学院', '23新闻D班', '8栋602', '13790971716', NULL, '主席团成员'),
('2023050402083', @pwd_user, '张静文', 2, '440783200411150626', '2023050402083', '人文社会科学学院', '23新闻D班', '凤翔3栋606', '13066260393', NULL, '主席团成员'),
('2023050402053', @pwd_user, '杜美婷', 2, '411729200412257262', '2023050402053', '人文社会科学学院', '23新闻D班', '凤翔3栋608', '15814668266', NULL, '主席团成员'),
('2023070103012', @pwd_user, '郑诗亭', 2, '445202200503063027', '2023070103012', '人文社会科学学院', '23法学a', '香晖b1205', '19840899982', NULL, '主席团成员'),
('2023050401056', @pwd_user, '钟利灵', 1, '441621200506260992', '2023050401056', '人文社会科学学院', '23新闻B班', '凤翔3-408', '18475684548', NULL, '主席团成员'),
('2023050402064', @pwd_user, '林思慧', 0, NULL, '2023050402064', NULL, NULL, NULL, NULL, NULL, '主席团成员'),
('2023070103020', @pwd_user, '廖盈莹', 2, '441322200603111724', '2023070103020', '经贸学院', '23金融C', '香晖b1205', '15018096566', NULL, '主席团成员'),
('2023020301011', @pwd_user, '刘恒铭', 1, '411002200503271539', '2023020301011', '机电工程学院', '23自动化a1', NULL, '19876851267', NULL, '主席团成员'),
('2023050401029', @pwd_user, '蔡玉儿', 2, '440515200504210926', '2023050401029', '人文社会科学学院', '23新闻A', '凤翔3栋309', '13527192269', NULL, '正式部员'),
('2025050601019', @pwd_user, '谭蘅萱', 2, '430225200606210163', '2025050601019', '人文社会科学学院', '25旅管A班', '香晖B321', '13149907621', NULL, '实习部员（二轮）'),
('2025060301067', @pwd_user, '李岚', 2, '440607200703140049', '2025060301067', '管理学院', '25人力资源管理B班', '香晖B1013', '13590929632', NULL, '实习部员（二轮）'),
('2025050401011', @pwd_user, '甘莉', 2, '441622200602204669', '2025050401011', '人文社会科学学院', '25新闻B班', '香晖B104', '13510328321', NULL, '实习部员（二轮）'),
('2025050402007', @pwd_user, '陈彤彤', 2, '441423200608290022', '2025050402007', '人文社会科学学院', '25新闻C班', '香晖B313', '13923023975', NULL, '实习部员（二轮）'),
('2025050402004', @pwd_user, '陈黄敏', 2, '440513200608224027', '2025050402004', '人文社会科学学院', '25新闻B班', '香晖B313', '15875413615', NULL, '实习部员（二轮）'),
('2025050402051', @pwd_user, '杨思涵', 2, '440512200707101229', '2025050402051', '人文社会科学学院', '25新闻C班', '香晖B316', '15218991715', NULL, '实习部员（二轮）'),
('2025050402045', @pwd_user, '谢泳珠', 2, '440507200603032723', '2025050402045', '人文社会科学学院', '25新闻C班', '香晖B319', '13556393720', NULL, '实习部员（二轮）'),
('2025050201034', @pwd_user, '许欣欣', 2, '440903200603020323', '2025050201034', '人文社会科学学院', '25行管A班', '香晖B210', '15875853640', NULL, '实习部员（二轮）'),
('2024050402064', @pwd_user, '张子萍', 2, '441481200608170024', '2024050402064', '人文社会科学学院', '24新闻C班', '香晖A828', '18320214527', NULL, '实习部员（二轮）'),
('2025050402039', @pwd_user, '陶晓君', 2, '441622200703050769', '2025050402039', '人文社会科学学院', '25新闻D班', '香晖B307', '13652528193', NULL, '实习部员（二轮）'),
('2024050401023', @pwd_user, '黄雅韵', 2, '440902200510152427', '2024050401023', '人文社会科学学院', '24新闻A班', '香晖A814', '15338487053', NULL, '实习部员（二轮）'),
('2025070304024', @pwd_user, '唐铭骏', 2, '350421200706017024', '2025070304024', '经贸学院', '25国贸D', '香晖c523', '13620090558', NULL, '实习部员（二轮）'),
('2025050402059', @pwd_user, '张晓莹', 2, '441882200610020023', '2025050402059', '人文社会科学学院', '25新闻D班', '香晖B318', '19820702178', NULL, '实习部员（二轮）'),
('2025050601035', @pwd_user, '陈璐', 2, '340521200706152027', '2025050601035', '人文社会科学学院', '25旅管B班', '香晖B813', '19277583648', NULL, '实习部员（二轮）'),
('2025050402020', @pwd_user, '康璐瑶', 2, '430503200611143020', '2025050402020', '人文社会科学学院', '25新闻c班', '香辉b307', '19865439792', NULL, '实习部员（二轮）'),
('2025030205058', @pwd_user, '徐旸凯', 1, '441224200606115410', '2025030205058', '计算机学院', '25软工F班', '12栋306', '13432445961', NULL, '实习部员（二轮）'),
('2025050402016', @pwd_user, '黄立欣', 2, '442000200705250887', '2025050402016', '人文社会科学学院', '25新闻D班', '香晖B314', '18924983430', NULL, '实习部员（二轮）'),
('2024040801034', @pwd_user, '张子豪', 1, '441424200511260536', '2024040801034', '电子信息学院', '24通信A班', '19栋607', '13826454513', NULL, '实习部员（二轮）'),
('2025030203021', @pwd_user, '黎林颖', 2, '440981200610174709', '2025030203021', '计算机学院', '25软工A班', '香晖A309', '19876967881', NULL, '实习部员（二轮）'),
('2025020101064', @pwd_user, '刘宇敏', 2, '440203200610091826', '2025020101064', '机电工程学院', '25电气A2班', '朝晖319', '15361330263', NULL, '实习部员（二轮）'),
('2025010401013', @pwd_user, '何志杰', 1, '360481200705092637', '2025010401013', '电子信息学院', '25光电', '13栋322', '18676962637', NULL, '实习部员（二轮）'),
('2025050102064', @pwd_user, '林子珊', 2, '440105200708157421', '2025050102064', '人文社会科学学院', '25法学D班', '香晖B126', '13662509898', NULL, '实习部员（二轮）'),
('2025050401053', @pwd_user, '张梦', 2, '440305200611057847', '2025050401053', '人文社会科学学院', '25新闻B班', '香晖B305', '19868278020', NULL, '实习部员（二轮）'),
('2025080301016', @pwd_user, '李沛家', 2, '440604200709130125', '2025080301016', '外国语学院', '25商英A班', '朝晖205', '15015568363', NULL, '实习部员（二轮）'),
('2025050102049', @pwd_user, '管梓莹', 2, '441422200707304227', '2025050102049', '人文社会科学学院', '25法学D班', '香晖B206', '18676991538', NULL, '实习部员（二轮）'),
('2025050401039', @pwd_user, '彭可昕', 2, '441523200703227609', '2025050401039', '人文社会科学学院', '25新闻A', '香晖B302', '19306604698', NULL, '实习部员（二轮）'),
('2025050201017', @pwd_user, '李满儿', 2, '445121200702012648', '2025050201017', '人文社会科学学院', '25行政管理A班', '香晖B208', '13534620667', NULL, '实习部员（二轮）'),
('2025040703058', @pwd_user, '何洁', 2, '440604200708150167', '2025040703058', '材料与食品学院', '25食品质量与安全B班', '香晖A423', '13827722788', NULL, '实习部员（二轮）'),
('2025050402043', @pwd_user, '肖可欣', 2, '445102200711051228', '2025050402043', '人文社会科学学院', '25新闻D班', '香晖B319', '19886485105', NULL, '实习部员（二轮）'),
('2025040704055', @pwd_user, '陈之茵', 2, '445281200502142482', '2025040704055', '材料与食品学院', '25食安D班', '香晖A912', '13113448076', NULL, '实习部员（二轮）'),
('2025040703022', @pwd_user, '袁咏仪', 2, '431021200609180021', '2025040703022', '材料与食品学院', '25食安A班', '香晖A407', '18575873985', NULL, '实习部员（二轮）'),
('2025050201079', @pwd_user, '周胡韵瑾', 2, '43102120070127002X', '2025050201079', '人文社会科学学院', '25行政管理b班', '香晖b214', '18627351875', NULL, '实习部员（二轮）'),
('2025050402042', @pwd_user, '吴宇萱', 2, '440304200610250040', '2025050402042', '人文社会科学学院', '25新闻d班', '香晖b315', '18038059732', NULL, '实习部员（二轮）'),
('2025090401005', @pwd_user, '陈乐琪', 2, '440903200711241609', '2025090401005', '艺术设计学院', '25视觉传达设计A班', '11栋202', '18027770267', NULL, '实习部员（二轮）'),
('2025090401049', @pwd_user, '张可盈', 2, '441423200702041444', '2025090401049', '艺术设计学院', '25视传A班', '11栋208', '18928461895', NULL, '实习部员（二轮）'),
('2025090401026', @pwd_user, '刘境宜', 2, '445221200610187825', '2025090401026', '艺术设计学院', '25视觉传达设计A班', '11栋202', '19075104060', NULL, '实习部员（二轮）'),
('2025050402030', @pwd_user, '林梓萌', 2, '445122200608045626', '2025050402030', '人文社会科学学院', '25新闻D班', '香晖B319', '13413747233', NULL, '实习部员（二轮）'),
('2025090501037', @pwd_user, '马雯', 2, '440403200704229129', '2025090501037', '艺术设计学院', '25工业设计', '香晖C611', '13005524506', NULL, '实习部员（二轮）'),
('2025010301058', @pwd_user, '温玉锴', 1, '440233200603083017', '2025010301058', '电子信息学院', '25通信A班', '13栋111', '17875548307', NULL, '实习部员（二轮）'),
('2025040802037', @pwd_user, '刘春霞', 2, '441781200704095921', '2025040802037', '材料与食品学院', '25应用化学D', '香晖A528', '15118013217', NULL, '实习部员（二轮）'),
('2025050402019', @pwd_user, '金思彤', 2, '330382200705261723', '2025050402019', '人文社会科学学院', '25新闻C班', '香晖B318', '18807607674', NULL, '实习部员（二轮）'),
('2025050402014', @pwd_user, '黄慧淳', 2, '445102200706131223', '2025050402014', '人文社会科学学院', '25新闻C班', '11-320', '15816522986', NULL, '实习部员（二轮）'),
('2025070201029', @pwd_user, '余牧遥', 2, '44051520070414162X', '2025070201029', '经贸学院', '25电子商务A班', '香晖C412', '13539675653', NULL, '实习部员（二轮）'),
('2025050401050', @pwd_user, '叶雪茵', 2, '440115200611040924', '2025050401050', '人文社会科学学院', '25新闻A班', '香晖B228', '15986308751', NULL, '实习部员（二轮）'),
('2025050601005', @pwd_user, '曾子琪', 2, '440883200507291963', '2025050601005', '人文社会科学学院', '25旅管A班', '香晖B324', '15107601071', NULL, '实习部员（二轮）'),
('2025020102069', @pwd_user, '王智旭', 1, '440305200701060417', '2025020102069', '机电工程学院', '25电气B2', '15栋303', '18952438257', NULL, '实习部员（二轮）'),
('2025070303025', @pwd_user, '邓婕', 2, '431021200707108648', '2025070303025', '经贸学院', '25国贸C班', '香晖C515', '15675661332', NULL, '实习部员（二轮）'),
('2024050104061', @pwd_user, '覃彤彬', 2, '430523200708146629', '2024050104061', '人文社会科学学院', '24法学F班', '香晖A720', '19925469068', NULL, '实习部员（二轮）'),
('2025060301003', @pwd_user, '陈羽悦', 2, '360721200704127623', '2025060301003', '管理学院', '25人力资源管理A班', '香晖B1006', '13798778713', NULL, '实习部员（二轮）'),
('2025010301035', @pwd_user, '廖宏泽', 1, '441284200705100050', '2025010301035', '电子信息学院', '25通信A班', '13栋103', '18078822982', NULL, '实习部员（二轮）'),
('2025020701043', @pwd_user, '郭安', 1, '14062120070326403X', '2025020701043', '机电工程学院', '25机电B', '15栋607', '13590998057', NULL, '实习部员（二轮）'),
('2025070302001', @pwd_user, '苏汉文', 1, '350212200704070019', '2025070302001', '经贸学院', '25国际经济与贸易B班', '13栋610', '19890395285', NULL, '实习部员（二轮）'),
('2025050401013', @pwd_user, '关雅文', 2, '440605200706270022', '2025050401013', '人文社会科学学院', '25新闻A班', '香晖B302', '18022228627', NULL, '实习部员（二轮）'),
('2025050402031', @pwd_user, '林曦晴', 2, '440114200709290220', '2025050402031', '人文社会科学学院', '25新闻D', '香晖B309', '13422167579', NULL, '实习部员（二轮）'),
('2025050102073', @pwd_user, '王锴悦', 2, '360321200706154527', '2025050102073', '人文社会科学学院', '25法学D班', '香晖苑b126', '13827276909', NULL, '实习部员（二轮）')
ON DUPLICATE KEY UPDATE
  `password` = VALUES(`password`),
  `real_name` = VALUES(`real_name`),
  `gender` = VALUES(`gender`),
  `id_card` = VALUES(`id_card`),
  `college` = VALUES(`college`),
  `class_name` = VALUES(`class_name`),
  `dorm_address` = VALUES(`dorm_address`),
  `phone` = VALUES(`phone`),
  `bank_card` = VALUES(`bank_card`),
  `remark` = VALUES(`remark`),
  `is_valid` = 1;

-- 2. 插入用户-角色关联。
-- 角色对应：管理员=0，主席团成员=1，留任部员=10，正式部员=14，二轮名单=9。
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `remark`)
SELECT u.id, r.id, x.remark
FROM (
  SELECT 'admin' AS username, 0 AS role_code, '管理员' AS remark UNION ALL
  SELECT '2023030103052' AS username, 1 AS role_code, '主席团成员' AS remark UNION ALL
  SELECT '2024050401003' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023070303020' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024070102016' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024070202009' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050301005' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024080405010' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050402022' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050402030' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024060101023' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050401032' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2024050401034' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050201020' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023080304017' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050402040' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050401038' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050402050' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050401056' AS username, 14 AS role_code, '正式成员' AS remark UNION ALL
  SELECT '2024050402054' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024030101078' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050402061' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050401064' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023050401068' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2024050401072' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023050401008' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2024040701012' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050401022' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023050401074' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2023050402057' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2023050402059' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2023080102025' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2024090101018' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023050401083' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2023050402067' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2023050402028' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2024080405021' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023010401044' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2024050103044' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023070103009' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2024050401052' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024060101020' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023020301072' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2024050601012' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024030102085' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024080406024' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '202308010125' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2024040802035' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024020301036' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024020302072' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2024070301014' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024060401015' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024070301016' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024080405001' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024090101001' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050402004' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024090301003' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050402010' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024010202012' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050402026' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050402038' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024090301019' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024070102001' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050402017' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023090402041' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2024070101019' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023070204024' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2024010201024' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023090401005' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050401007' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024030801006' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023090402011' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024020701018' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2023080401003' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024060101034' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024030202094' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050402046' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024090402028' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023050401078' AS username, 10 AS role_code, '留任部员' AS remark UNION ALL
  SELECT '2024020102069' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050402059' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023050402038' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024090501049' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024050402068' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024010202072' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2024090101043' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2024050401071' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2023050401072' AS username, 1 AS role_code, '主席团成员' AS remark UNION ALL
  SELECT '2023050401063' AS username, 1 AS role_code, '主席团成员' AS remark UNION ALL
  SELECT '2023060102006' AS username, 1 AS role_code, '主席团成员' AS remark UNION ALL
  SELECT '2023080402027' AS username, 1 AS role_code, '主席团成员' AS remark UNION ALL
  SELECT '2023050402060' AS username, 1 AS role_code, '主席团成员' AS remark UNION ALL
  SELECT '2023050402083' AS username, 1 AS role_code, '主席团成员' AS remark UNION ALL
  SELECT '2023050402053' AS username, 1 AS role_code, '主席团成员' AS remark UNION ALL
  SELECT '2023070103012' AS username, 1 AS role_code, '主席团成员' AS remark UNION ALL
  SELECT '2023050401056' AS username, 1 AS role_code, '主席团成员' AS remark UNION ALL
  SELECT '2023050402064' AS username, 1 AS role_code, '主席团成员' AS remark UNION ALL
  SELECT '2023070103020' AS username, 1 AS role_code, '主席团成员' AS remark UNION ALL
  SELECT '2023020301011' AS username, 1 AS role_code, '主席团成员' AS remark UNION ALL
  SELECT '2023050401029' AS username, 14 AS role_code, '正式部员' AS remark UNION ALL
  SELECT '2025050601019' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025060301067' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050401011' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402007' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402004' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402051' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402045' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050201034' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2024050402064' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402039' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2024050401023' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025070304024' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402059' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050601035' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402020' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025030205058' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402016' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2024040801034' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025030203021' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025020101064' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025010401013' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050102064' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050401053' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025080301016' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050102049' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050401039' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050201017' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025040703058' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402043' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025040704055' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025040703022' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050201079' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402042' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025090401005' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025090401049' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025090401026' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402030' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025090501037' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025010301058' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025040802037' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402019' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402014' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025070201029' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050401050' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050601005' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025020102069' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025070303025' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2024050104061' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025060301003' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025010301035' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025020701043' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025070302001' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050401013' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050402031' AS username, 9 AS role_code, '实习部员（二轮）' AS remark UNION ALL
  SELECT '2025050102073' AS username, 9 AS role_code, '实习部员（二轮）' AS remark
) x
JOIN `sys_user` u ON u.username = x.username
JOIN `sys_role` r ON r.role_code = x.role_code
ON DUPLICATE KEY UPDATE `remark` = VALUES(`remark`);

-- 3. 插入用户-部门关联。
-- 部门对应：主席团=PRESIDIUM，采编部=EDITORIAL，摄影部=PHOTOGRAPHY，视觉部/视觉传达与技术部=VISUAL，视频部=VIDEO。
INSERT INTO `sys_user_dept` (`user_id`, `dept_id`, `remark`)
SELECT u.id, d.id, x.remark
FROM (
  SELECT '2023030103052' AS username, 'PRESIDIUM' AS dept_code, '主席团' AS remark UNION ALL
  SELECT '2024050401003' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2023070303020' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024070102016' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024070202009' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050301005' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024080405010' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050402022' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050402030' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024060101023' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050401032' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050401034' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050201020' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2023080304017' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050402040' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050401038' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050402050' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050401056' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050402054' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024030101078' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050402061' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050401064' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2023050401068' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050401072' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2023050401008' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024040701012' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024050401022' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2023050401074' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2023050402057' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2023050402059' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2023080102025' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024090101018' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2023050401083' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2023050402067' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2023050402028' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024080405021' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2023010401044' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024050103044' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2023070103009' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024050401052' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024060101020' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2023020301072' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024050601012' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024030102085' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024080406024' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '202308010125' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024040802035' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024020301036' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024020302072' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024070301014' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024060401015' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024070301016' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024080405001' AS username, 'VISUAL' AS dept_code, '视觉部' AS remark UNION ALL
  SELECT '2024090101001' AS username, 'VISUAL' AS dept_code, '视觉部' AS remark UNION ALL
  SELECT '2024050402004' AS username, 'VISUAL' AS dept_code, '视觉部' AS remark UNION ALL
  SELECT '2024090301003' AS username, 'VISUAL' AS dept_code, '视觉部' AS remark UNION ALL
  SELECT '2024050402010' AS username, 'VISUAL' AS dept_code, '视觉部' AS remark UNION ALL
  SELECT '2024010202012' AS username, 'VISUAL' AS dept_code, '视觉部' AS remark UNION ALL
  SELECT '2024050402026' AS username, 'VISUAL' AS dept_code, '视觉部' AS remark UNION ALL
  SELECT '2024050402038' AS username, 'VISUAL' AS dept_code, '视觉部' AS remark UNION ALL
  SELECT '2024090301019' AS username, 'VISUAL' AS dept_code, '视觉部' AS remark UNION ALL
  SELECT '2024070102001' AS username, 'VISUAL' AS dept_code, '视觉部' AS remark UNION ALL
  SELECT '2024050402017' AS username, 'VISUAL' AS dept_code, '视觉部' AS remark UNION ALL
  SELECT '2023090402041' AS username, 'VISUAL' AS dept_code, '视觉部' AS remark UNION ALL
  SELECT '2024070101019' AS username, 'VISUAL' AS dept_code, '视觉部' AS remark UNION ALL
  SELECT '2023070204024' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024010201024' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2023090401005' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024050401007' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024030801006' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2023090402011' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024020701018' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2023080401003' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024060101034' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024030202094' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024050402046' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024090402028' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2023050401078' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024020102069' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024050402059' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2023050402038' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024090501049' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024050402068' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024010202072' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024090101043' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024050401071' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2023050401072' AS username, 'PRESIDIUM' AS dept_code, '主席团' AS remark UNION ALL
  SELECT '2023050401063' AS username, 'PRESIDIUM' AS dept_code, '主席团' AS remark UNION ALL
  SELECT '2023060102006' AS username, 'PRESIDIUM' AS dept_code, '主席团' AS remark UNION ALL
  SELECT '2023080402027' AS username, 'PRESIDIUM' AS dept_code, '主席团' AS remark UNION ALL
  SELECT '2023050402060' AS username, 'PRESIDIUM' AS dept_code, '主席团' AS remark UNION ALL
  SELECT '2023050402083' AS username, 'PRESIDIUM' AS dept_code, '主席团' AS remark UNION ALL
  SELECT '2023050402053' AS username, 'PRESIDIUM' AS dept_code, '主席团' AS remark UNION ALL
  SELECT '2023070103012' AS username, 'PRESIDIUM' AS dept_code, '主席团' AS remark UNION ALL
  SELECT '2023050401056' AS username, 'PRESIDIUM' AS dept_code, '主席团' AS remark UNION ALL
  SELECT '2023050402064' AS username, 'PRESIDIUM' AS dept_code, '主席团' AS remark UNION ALL
  SELECT '2023070103020' AS username, 'PRESIDIUM' AS dept_code, '主席团' AS remark UNION ALL
  SELECT '2023020301011' AS username, 'PRESIDIUM' AS dept_code, '主席团' AS remark UNION ALL
  SELECT '2023050401029' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025050601019' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2025060301067' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2025050401011' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2025050402007' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2025050402004' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2025050402051' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2025050402045' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2025050201034' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050402064' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2025050402039' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2024050401023' AS username, 'EDITORIAL' AS dept_code, '采编部' AS remark UNION ALL
  SELECT '2025070304024' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2025050402059' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2025050601035' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2025050402020' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2025030205058' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2025050402016' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2024040801034' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2025030203021' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2025020101064' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2025010401013' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2025050102064' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2025050401053' AS username, 'PHOTOGRAPHY' AS dept_code, '摄影部' AS remark UNION ALL
  SELECT '2025080301016' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025050102049' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025050401039' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025050201017' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025040703058' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025050402043' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025040704055' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025040703022' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025050201079' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025050402042' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025090401005' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025090401049' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025090401026' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025050402030' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025090501037' AS username, 'VISUAL' AS dept_code, '视觉传达与技术部' AS remark UNION ALL
  SELECT '2025010301058' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025040802037' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025050402019' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025050402014' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025070201029' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025050401050' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025050601005' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025020102069' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025070303025' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2024050104061' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025060301003' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025010301035' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025020701043' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025070302001' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025050401013' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025050402031' AS username, 'VIDEO' AS dept_code, '视频部' AS remark UNION ALL
  SELECT '2025050102073' AS username, 'VIDEO' AS dept_code, '视频部' AS remark
) x
JOIN `sys_user` u ON u.username = x.username
JOIN `sys_dept` d ON d.dept_code = x.dept_code
ON DUPLICATE KEY UPDATE `remark` = VALUES(`remark`);

-- 4. 快速校验查询（可选）：导入后可以执行下面这些 SELECT 检查数量。
-- SELECT COUNT(*) AS user_count FROM sys_user;
-- SELECT r.role_code, r.role_name, COUNT(*) AS cnt FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id GROUP BY r.role_code, r.role_name ORDER BY r.role_code;
-- SELECT d.dept_code, d.dept_name, COUNT(*) AS cnt FROM sys_user_dept ud JOIN sys_dept d ON d.id = ud.dept_id GROUP BY d.dept_code, d.dept_name ORDER BY d.dept_code;
