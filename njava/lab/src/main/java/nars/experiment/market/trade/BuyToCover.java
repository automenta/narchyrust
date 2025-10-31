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
// * Allows the user to buy back the stock they sold short as to get off margin
// */
//public class BuyToCover extends Trade {
//
//    public BuyToCover(Account a) {
//        super();
//
//        try {
//            //validation on stock symbol
//            Scanner user_input = new Scanner(System.in);
//
//            System.out.println("Enter stock symbol you would like to trade: ");
//            symbol = user_input.nextLine();
//
//            Stock stock = Market.stock(symbol, Market.stocks);
//
//            if (stock == null) {
//                System.out.println("Sorry that stock is not trading.");
//
//            } else {
//
//                System.out.println("How many shares would you like to trade: ");
//                quantity = user_input.nextInt();
//
//                // validation on quantity
//                if (quantity <= stock.available) {
//                    double totalPrice = stock.price * quantity;
//                    if (a.cash >= totalPrice) {
//                        System.out.println("The total comes to: \u00A3" + new DecimalFormat("#.00").format(totalPrice));
//                        throw new RuntimeException("TODO");
//                    } else {
//                        System.out.println("insufficient cash");
//                    }
//                } else {
//                    System.out.println("insufficient shares");
//                }
//            }
//
//        } catch (RuntimeException ex) {
//            System.out.println("Invalid trade");
//        }
//
//    } // end buy constuctor
//
//}