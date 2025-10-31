//package nars.experiment.market.trade;
//
//import nars.experiment.market.Account;
//import nars.experiment.market.Market;
//import nars.experiment.market.Stock;
//import nars.experiment.market.Trade;
//
//import java.text.DecimalFormat;
//import java.util.Scanner;
//
///**
// * AUTHOR: Bradley Winter
// * <p>
// * Allows the user to trade stocks worth more than their total account value
// * this is called trading on margin and is an advance feature for this simulator
// */
//public class SellShort extends Trade {
//
//    public SellShort(Account ac) {
//        super();
//
//        try {
//            //validation on stock symbol
//            Scanner user_input = new Scanner(System.in);
//
//            System.out.println("Enter stock symbol you would like to trade: ");
//            symbol = user_input.nextLine();
//
//            Stock stockTrade = Market.stock(symbol, Market.stocks);
//
//            if (stockTrade == null) {
//                System.out.println("Sorry that stock is not trading.");
//
//            } else {
//
//                System.out.println("How many shares would you like to trade: ");
//                quantity = user_input.nextInt();
//
//                // validation on quantity
//                if (quantity <= stockTrade.available) {
//                    double totalPrice = stockTrade.price * quantity;
//                    System.out.println("The total comes to: \u00A3" + new DecimalFormat("#.00").format(totalPrice));
//                    throw new RuntimeException("TODO");
//                } else {
//                    System.out.println("Sorry not enough shares available.");
//                }
//            }
//
//        } catch (RuntimeException ex) {
//            System.out.println("Error trade not gone through");
//        }
//
//    } // end sell short constuctor
//
//}