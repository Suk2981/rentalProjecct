package controller;

import static util.Session.userLoginId;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import dao.RentalDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import model.RentalHistoryDTO;

public class RentalHistoryController implements Initializable {

	@FXML
	private TableColumn<RentalHistoryDTO, Integer> indexCol;
	@FXML
	private TableView<RentalHistoryDTO> rentalTable;
	@FXML
	private TableColumn<RentalHistoryDTO, String> eqNameCol;
	@FXML
	private TableColumn<RentalHistoryDTO, String> officeNameCol;
	@FXML
	private TableColumn<RentalHistoryDTO, LocalDateTime> rentalDateCol; // 대여일 / 반납일
	@FXML
	private TableColumn<RentalHistoryDTO, String> returnStatusCol; // 상태
	@FXML
	private TableColumn<RentalHistoryDTO, Long> overdueDaysCol; // 연체일 / 연체료
	@FXML
	private TableColumn<RentalHistoryDTO, Void> returnActionCol; // 반납하기 버튼

	@FXML
	private Button eqList;
	@FXML
	private Button myInfoButton;

	private RentalDAO rentalDAO = new RentalDAO();

	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm");
	private final DateTimeFormatter dateOnlyFormatter = DateTimeFormatter.ofPattern("yy-MM-dd");

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		indexCol.setCellFactory(column -> new TableCell<RentalHistoryDTO, Integer>() {
			@Override
			protected void updateItem(Integer item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setText(null);
				} else {
					setText(String.valueOf(getIndex() + 1));
					setAlignment(Pos.CENTER);
				}
			}
		});

		eqNameCol.setCellValueFactory(new PropertyValueFactory<>("eqName"));
		officeNameCol.setCellValueFactory(new PropertyValueFactory<>("officeName"));

		rentalDateCol.setCellFactory(param -> new TableCell<RentalHistoryDTO, LocalDateTime>() {
			@Override
			protected void updateItem(LocalDateTime item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
					setText(null);
				} else {
					RentalHistoryDTO rental = getTableView().getItems().get(getIndex());
					VBox vbox = new VBox(2);
					vbox.setAlignment(Pos.CENTER);
					Text rentalDateText = new Text(rental.getRentalDate().format(dateOnlyFormatter));
					Text actualReturnDateText = new Text();
					if (rental.getActualReturnDate() != null) {
						actualReturnDateText.setText(rental.getActualReturnDate().format(dateOnlyFormatter));
					} else {
						actualReturnDateText.setText("");
					}
					vbox.getChildren().addAll(rentalDateText, actualReturnDateText);
					setGraphic(vbox);
					setText(null);
					setAlignment(Pos.CENTER);
				}
			}
		});

		overdueDaysCol.setCellFactory(param -> new TableCell<RentalHistoryDTO, Long>() {
			@Override
			protected void updateItem(Long item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
					setText(null);
				} else {
					RentalHistoryDTO rental = getTableView().getItems().get(getIndex());
					VBox vbox = new VBox(2);
					vbox.setAlignment(Pos.CENTER);
					Text overdueDaysText = new Text(rental.getOverdueDays() + "일");
					Text overdueFeeText = new Text(String.format("%,d원", rental.getOverdueFee()));
					vbox.getChildren().addAll(overdueDaysText, overdueFeeText);
					setGraphic(vbox);
					setText(null);
					setAlignment(Pos.CENTER);
				}
			}
		});

		returnActionCol.setCellFactory(param -> new TableCell<RentalHistoryDTO, Void>() {
			private final Button returnButton = new Button();

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					RentalHistoryDTO rental = getTableView().getItems().get(getIndex());

					if ("반납완료".equals(rental.getReturnStatus())) {
						returnButton.setText("반납완료");
						returnButton.setStyle(
								"-fx-background-color: #f0f0f0; -fx-text-fill: #555555; -fx-background-radius: 20; -fx-border-radius: 20;");
						returnButton.setDisable(true);
					} else if ("대여중".equals(rental.getReturnStatus())) {
						returnButton.setText("반납하기");
						returnButton.setStyle(
								"-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 20; -fx-border-radius: 20;");
						returnButton.setDisable(false);

						returnButton.setOnAction(event -> {
							System.out.println("반납하기 클릭: " + rental.getRentalNum() + " - " + rental.getEqName());

							// 중요: 최신 연체료 정보를 다시 로드하는 로직이 필요할 수 있습니다.
							// 현재는 DTO의 overdueFee를 사용하지만, 클릭 시점에 DB의 최신값을 조회하는 것이 안전합니다.
							// 예시에서는 DTO의 값을 사용합니다.
							long currentOverdueFee = rental.getOverdueFee();

							if (currentOverdueFee > 0) {
								showOverduePaymentProcess(rental); // 연체료 결제 프로세스 시작
							} else {
								processReturnOnly(rental); // 일반 반납 처리
							}
						});

					} else {
						setGraphic(null);
						return;
					}

					returnButton.setPrefWidth(110);
					returnButton.setPrefHeight(25);
					setAlignment(Pos.CENTER);
					setGraphic(returnButton);
				}
			}
		});

		loadRentalHistory();
	}

	private void loadRentalHistory() {
		List<RentalHistoryDTO> list = rentalDAO.findRentalsByUserId(userLoginId);
		ObservableList<RentalHistoryDTO> observableList = FXCollections.observableArrayList(list);
		rentalTable.setItems(observableList);
	}

	// ========== 연체료 결제 프로세스 시작 메서드 (새로 추가되거나 수정됨) ==========
	private void showOverduePaymentProcess(RentalHistoryDTO rental) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("연체료 납부");
		// rental.getSerialNum()이 DTO에 없다면 rental.getRentalNum()으로 대체하세요.
		alert.setHeaderText(rental.getEqName() + " (대여번호: " + rental.getRentalNum() + ")");
		alert.setContentText("이 장비는 현재 연체되었습니다.\n" + "연체일: " + rental.getOverdueDays() + "일\n" + "납부할 연체료: "
				+ String.format("%,d원", rental.getOverdueFee()) + "\n\n" + "지금 결제하시겠습니까?");

		ButtonType payButton = new ButtonType("결제 진행");
		ButtonType cancelButton = new ButtonType("취소", ButtonType.CANCEL.getButtonData());

		alert.getButtonTypes().setAll(payButton, cancelButton);

		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent() && result.get() == payButton) {
			// 실제 결제창을 띄우는 로직 (현재는 시뮬레이션)
			boolean paymentSuccess = simulatePayment(rental.getOverdueFee());

			if (paymentSuccess) {
				showAlert("결제 성공", null,
						"연체료 " + String.format("%,d원", rental.getOverdueFee()) + " 결제가 완료되었습니다.\n이제 장비를 반납 처리합니다.");

				// 결제 성공 후 연체료를 0으로 업데이트하는 DAO 호출
				String updateResult = rentalDAO.updateOverdueFeeToZero(rental.getRentalNum());
				if ("SUCCESS".equals(updateResult)) {
					System.out.println("연체료 DB 업데이트 성공.");
					// 연체료 업데이트 성공 후 최종 반납 처리 진행
					processReturnOnly(rental);
				} else {
					showAlert("오류", null, "연체료 정산 중 오류가 발생했습니다: " + updateResult + ". 관리자에게 문의하세요.");
				}
			} else {
				showAlert("결제 실패", null, "연체료 결제에 실패했습니다. 다시 시도해 주세요.");
			}
		} else {
			System.out.println("연체료 결제 취소.");
		}
	}

	// ========== 결제 시뮬레이션 메서드 (새로 추가됨) ==========
	private boolean simulatePayment(long amount) {
		Alert confirmPayment = new Alert(AlertType.CONFIRMATION);
		confirmPayment.setTitle("결제 시뮬레이션");
		confirmPayment.setHeaderText(String.format("연체료 %,d원 결제 진행", amount));
		confirmPayment.setContentText("실제로 결제하시겠습니까? (성공/실패 선택)");

		ButtonType successButton = new ButtonType("결제 성공");
		ButtonType failButton = new ButtonType("결제 실패");
		confirmPayment.getButtonTypes().setAll(successButton, failButton);

		Optional<ButtonType> result = confirmPayment.showAndWait();
		return result.isPresent() && result.get() == successButton;
	}

	// ========== 순수 반납 처리 로직 메서드 (새로 추가되거나 수정됨) ==========
	private void processReturnOnly(RentalHistoryDTO rental) {
		String resultStatus = rentalDAO.processReturn(rental.getRentalNum());

		switch (resultStatus) {
		case "SUCCESS":
			showAlert("반납 완료", null, rental.getEqName() + " 장비 반납이 성공적으로 처리되었습니다.");
			break;
		case "ALREADY_RETURNED":
			showAlert("반납 실패", null, "이미 반납이 완료된 장비입니다.");
			break;
		case "NOT_FOUND":
			showAlert("오류", null, "해당 대여 기록을 찾을 수 없습니다.");
			break;
		case "ERROR": // 기타 SQL 에러 등
		default:
			showAlert("오류", null, "알 수 없는 오류로 반납에 실패했습니다: " + resultStatus + ". 관리자에게 문의하세요.");
			break;
		}
		loadRentalHistory(); // 테이블 갱신
	}

	// ========== 내비게이션 메서드 (기존과 동일, 변경 없음) ==========
	public void handleEqList(ActionEvent event) {
		try {
			Parent mainView = FXMLLoader.load(getClass().getResource("/view/MainView.fxml"));
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.setScene(new Scene(mainView));
			stage.setTitle("MainView");
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void handleMyInfo(ActionEvent event) {
		try {
			Parent myInfoView = FXMLLoader.load(getClass().getResource("/view/MyInfoView.fxml"));
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			stage.setScene(new Scene(myInfoView));
			stage.setTitle("내 정보");
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ========== 일반적인 알림창 메서드 ==========
	private void showAlert(String title, String header, String content) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}
}
