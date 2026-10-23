# Tử vi 12 con giáp và phong thủy — bộ quy tắc 1.1

Mã: `lichviet-cultural-1.1-v1`; diễn giải: `branch-reflection-v1`.
Đối chiếu nguồn trực tuyến ngày 23/10/2026. Các kết quả được tính offline,
không gọi API sinh lời luận, không dùng số ngẫu nhiên hoặc điểm may mắn.

## Nguồn và phần thực sự sử dụng

| Nguồn | Phần sử dụng | Phần không quy cho nguồn |
| --- | --- | --- |
| [British Library / International Dunhuang Programme: Animals of the Chinese zodiac](https://idp.bl.uk/discover/learning/chinese-astronomy/articles/astrology-and-astronomy/animals-of-the-chinese-zodiac/) | Tóm lược rất ngắn biểu tượng tính cách của 12 vị trí con giáp | Không sao chép bài, tranh, bảng năm hoặc bảng ngũ hành của trang; trang có một số năm không nhất quán. Ngày và năm do core tính. |
| [VietnamPlus / TTXVN: Cat instead of Rabbit](https://en.vietnamplus.vn/why-does-vietnam-celebrate-the-year-of-the-cat-instead-of-the-rabbit-post255154.vnp) | Quy ước Việt Nam dùng Mèo và Trâu | Các đặc điểm chuyển dịch sang Mèo/Trâu là biên tập văn hóa, không phải kết luận nhân cách. |
| [Tam Mệnh Thông Hội, quyển II](https://zh.wikisource.org/wiki/三命通會_(四庫全書本)/卷02) | 論支元六合, 論支元三合, 論衝擊: sáu cặp hợp, bốn nhóm tam hợp, sáu cặp xung | Cổ thư không viết các câu dự báo ngày/tuần/tháng/năm cụ thể đang xuất hiện trong app. Lời luận là diễn giải của Lịch Việt. |
| [Tam Mệnh Thông Hội, quyển I](https://zh.wikisource.org/wiki/三命通會_(四庫全書本)/卷01) | 論五行生尅, 總論納音, 論納音取象: quan hệ sinh/khắc và 30 cặp nạp âm | Không coi nạp âm, ngũ hành thiên can và ngũ hành địa chi là một trường dữ liệu. |
| [Hong Kong Observatory: Heavenly Stems and Earthly Branches](https://www.weather.gov.hk/en/gts/time/stemsandbranches.htm) | Bảng 2: chu kỳ 60; bảng 3: 12 giờ kép; bảng 4–5: can tháng và can giờ | Không dùng lịch Trung Quốc thay lịch Việt. Ranh ngày Tý là quy ước đầu vào riêng của app. |
| [Hiệp Kỷ Biện Phương Thư, quyển II](https://zh.wikisource.org/wiki/欽定協紀辨方書_(四庫全書本)/卷02) | 二十四方位, 正五行, 大遊年變卦: 24 Sơn, chính ngũ hành, biến ba hào của Đại Du Niên | Không trộn Tiểu Du Niên, Hồng Phạm hoặc song sơn ngũ hành; không tự xem cung phi người ở. |
| [Hiệp Kỷ Biện Phương Thư, quyển VII](https://zh.wikisource.org/wiki/欽定協紀辨方書_(四庫全書本)/卷07) | 黄道黒道: thứ tự 12 thần, sáu hoàng đạo và khởi Thanh Long theo chi ngày | Không biến tên thần hoặc cát/hung truyền thống thành dự đoán chắc chắn. |

Giao diện VI/EN có nút nguồn ngay tại từng nhóm kết quả, nhan đề, mục sách,
phạm vi sử dụng và URL có thể chọn/sao chép. Nội dung giải thích thiết yếu
được đóng gói offline; chỉ mở bản nguồn bên ngoài mới cần trình duyệt/mạng.
Không tải ảnh hoặc sao chép bản dịch dài từ các trang nguồn.

## Tuổi, nạp âm và quan hệ

- Ngày sinh dương được đổi sang âm lịch Việt UTC+7; tuổi đổi vào Tết, không
  đổi vào 01/01 hoặc Lập Xuân. Có đủ 12 vị trí, dùng Mèo/Trâu theo Việt Nam.
- Một tuổi có năm tổ hợp thiên can trong chu kỳ 60. Hiển thị đủ năm dòng
  Can Chi/nạp âm và các **nhãn năm âm** 1899–2100. Đây không phải ngày sinh
  dương; ngày đầu 1900 có thể còn thuộc năm âm 1899.
- Nạp âm lấy từng cặp liên tiếp Giáp Tý/Ất Sửu, Bính Dần/Đinh Mão…; kiểm tra
  cùng tính chẵn/lẻ giữa can và chi trước khi tra. Nạp âm có 30 tên, mỗi tên
  gắn một hành. Dùng tên Việt thông dụng; Đại Trạch Thổ còn gọi Đại Dịch Thổ.
- Lục hợp: Tý–Sửu, Dần–Hợi, Mão–Tuất, Thìn–Dậu, Tỵ–Thân, Ngọ–Mùi.
- Tam hợp: Thân–Tý–Thìn, Tỵ–Dậu–Sửu, Dần–Ngọ–Tuất, Hợi–Mão–Mùi. App ghi
  **cùng nhóm** cho hai chi, không tuyên bố đủ ba chi hoặc đã hóa cục.
- Lục xung: hai chi cách nhau sáu vị trí. Không gọi mọi cặp trong một bộ
  tứ hành xung là đối xung; không suy luận mức độ hợp hôn nhân từ hai năm.
- Sinh: Mộc→Hỏa→Thổ→Kim→Thủy→Mộc. Khắc: Mộc→Thổ→Thủy→Hỏa→Kim→Mộc.
  Quan hệ có hướng: Kim sinh Thủy khác Thủy được Kim sinh.

## Bốn chu kỳ dự báo

Đây là **bản đọc biểu tượng dân gian có phép tính**, không phải bài dự báo
thời sự được cấp phép từ nhà xuất bản. `ZodiacForecast` giữ riêng đầu vào,
khoảng thời gian, Can Chi, quan hệ, kết quả từng ngày và ID nguồn. Câu chữ
biên tập của app nằm trong resources VI/EN; không gán chúng cho cổ thư.

| Chu kỳ | Khoảng hiển thị | Căn cứ lời luận chính |
| --- | --- | --- |
| Ngày | Một ngày dân dụng Việt Nam | Chi tuổi so với chi ngày |
| Tuần | Thứ Hai đến Chủ Nhật, có thể qua Tết | Tổng hợp từng ngày; không tạo Can Chi tuần |
| Tháng | Tháng âm đang chứa ngày tham chiếu, tách tháng nhuận | Chi tuổi so với chi tháng âm |
| Năm | Tết đến ngày trước Tết tiếp theo | Chi tuổi so với chi năm âm |

Lục hợp/cùng nhóm tam hợp → chủ đề phối hợp; xung → rà soát; đồng chi →
củng cố; còn lại → cân bằng. Tuần có cả hợp và xung → đan xen, chỉ có hợp →
phối hợp, chỉ có xung → rà soát, không có cả hai → cân bằng. Đây là quy ước
biên tập **được công bố**, không phải phép tính xác suất vận may.

Mỗi kết quả có gợi ý công việc/giao tiếp do app viết, năm/tháng bối cảnh,
số ngày từng nhóm và bảng từng ngày có thể mở lại lịch. Cùng đầu vào cho
cùng kết quả. Không sinh dự báo bệnh tật, lợi nhuận, sự kiện đời người hoặc
những xác suất không có cơ sở. Khi chu kỳ vượt 1900–2100, chỉ tính đoạn nằm
trong phạm vi và đánh dấu chu kỳ bị cắt.

## Giờ và phương vị

- Can giờ Tý theo can ngày: Giáp/Kỷ→Giáp, Ất/Canh→Bính, Bính/Tân→Mậu,
  Đinh/Nhâm→Canh, Mậu/Quý→Nhâm; mỗi chi tiếp tăng một can. UTC+7 dân dụng,
  không hiệu chỉnh giờ chân thái dương.
- Tý là 23–01. Bảng ngày dân dụng tách 00–01 và 23–24 thành 13 đoạn phủ
  đúng 24 giờ. Quy ước 00h giữ can ngày hiện tại; quy ước 23h dùng can ngày
  kế tiếp và tính lại giờ hoàng đạo. Không vượt âm thầm 2100-12-31.
- Thanh Long: Dần/Thân khởi Tý, Mão/Dậu khởi Dần, Thìn/Tuất khởi Thìn,
  Tỵ/Hợi khởi Ngọ, Tý/Ngọ khởi Thân, Sửu/Mùi khởi Tuất. Sáu vị trí từ
  Thanh Long là 0,1,4,5,7,10. Dùng cùng engine với lịch/widget hiện có.
- 24 Sơn chính châm: Tý tại 0°, Quý 15°… Nhâm 345°, mỗi khoảng rộng 15°;
  ranh dưới gồm, ranh trên không gồm. Bắc 0°, Đông 90°, Nam 180°, Tây 270°.
  Không xin quyền cảm biến/vị trí, không tự hiệu chỉnh từ sai; đầu vào là
  góc do người dùng nhập với cùng một mốc Bắc.
- Chọn **trạch quái theo tọa**, tọa = mặt tiền+180°. Không coi quái mặt
  tiền là quái tọa hoặc quái năm sinh. App nêu rõ quy ước ngay trên kết quả.
- Đại Du Niên: hào dưới là bit 0. XOR quái tọa/quái phương = 0 Phục Vị,
  4 Sinh Khí, 3 Thiên Y, 7 Diên Niên, 1 Họa Hại, 6 Ngũ Quỷ, 5 Lục Sát,
  2 Tuyệt Mệnh. Đối chiếu đầy đủ ma trận 8×8, không thay bằng Tiểu Du Niên.
  Tên gọi lịch sử không được diễn thành kết luận sức khỏe/tài sản.

## Phạm vi còn lại

V1.1.0 hoàn thiện màn **12 con giáp** và bốn chu kỳ trong bộ quy tắc này.
Không tuyên bố hỗ trợ mọi trường phái tử vi/phong thủy. Lá số Đẩu Số giữ
phạm vi riêng trong [HOROSCOPE_RULES.md](../core/HOROSCOPE_RULES.md).
Nhị thập bát tú, Huyền Không phi tinh, Kỳ Môn, cung phi người ở, luận
độ sáng sao và dự báo lá số chuyên sâu chưa được triển khai ở bản này.

Room schema 4 / backup JSON 5 không đổi. Tra cứu mới không lưu ngày sinh
vào cơ sở dữ liệu; trạng thái màn hình được lưu qua recreation/chuyển tab.
Không thêm quyền, dịch vụ nền hoặc thư viện mạng.
