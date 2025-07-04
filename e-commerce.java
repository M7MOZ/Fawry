import java.time.LocalDate;
import java.util.*;

class Product {
    protected String name;
    private double price;
    private int quantity;

    public Product(String name, double price, int quantity){
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }

    public void reduceQuantity(int amount) {
        if (quantity >= amount) {
            quantity -= amount;
        }
    }
}

interface Expirable {
    LocalDate getExpiryDate();
    boolean isExpired();
    String getName();
}

interface Shippable {
    double getWeight();    
    String getName();
}

class ExpirableShippableProduct extends Product implements Expirable, Shippable {

    private LocalDate expiryDate;
    private double weight;

    public ExpirableShippableProduct(String name, double price, int quantity, LocalDate expiryDate, double weight) {
        super(name, price, quantity);
        this.expiryDate = expiryDate;
        this.weight = weight;
    }

    @Override
    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    @Override
    public String getName() { return name; }
}

class ExpirableProduct extends Product implements Expirable {

    private LocalDate expiryDate;

    public ExpirableProduct(String name, double price, int quantity, LocalDate expiryDate) {
        super(name, price, quantity);
        this.expiryDate = expiryDate;
    }

    @Override
    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    @Override
    public boolean isExpired () {
        return LocalDate.now().isAfter(expiryDate);
    }

    @Override
    public String getName() { return name; }
}

class ShippableProduct extends Product implements Shippable {

    private double weight;

    public ShippableProduct(String name, double price, int quantity, double weight) {
        super(name, price, quantity);
        this.weight = weight;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public String getName() { return name; }
}

class Customer {
    private String name;
    private double balance;

    public Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }

    public void deductBalance(double amount) {
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        balance -= amount;
    }
}

class CartItem {
    private Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public double getTotalPrice() { return product.getPrice() * quantity; }
}

class Cart {
    private List<CartItem> items;

    public Cart() {
        this.items = new ArrayList<>();
    }

    public void add(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (quantity > product.getQuantity()) {
            throw new IllegalArgumentException("Requested quantity exceeds available stock");
        }

        if (product instanceof Expirable) {
            Expirable exp = (Expirable) product;
            if (exp.isExpired()) {
                throw new IllegalArgumentException("Cannot add expired product to cart");
            }
        }

        for (CartItem item : items) {
            if (item.getProduct().equals(product)) {
                int newQuantity = item.getQuantity() + quantity;
                if (newQuantity > product.getQuantity()) {
                    throw new IllegalArgumentException("Total quantity exceeds available stock");
                }
                items.remove(item);
                items.add(new CartItem(product, newQuantity));
                return;
            }
        }

        items.add(new CartItem(product, quantity));
    }

    public List<CartItem> getItems() {
        return new ArrayList<>(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public double getSubtotal() {
        double subtotal = 0;
        for (CartItem item : items) {
            subtotal += item.getTotalPrice();
        }
        return subtotal;
    }
}

class ShippingService {

    private static final double SHIPPING_RATE_PER_KG = 10.0;

    public static double calculateShippingFee(List<Shippable> shippableItems) {
        if (shippableItems.isEmpty()) {
            return 0;
        }

        double totalWeight = 0;

        for (Shippable item : shippableItems) {
            totalWeight += item.getWeight();
        }

        return totalWeight * SHIPPING_RATE_PER_KG;
    }
}

class CheckoutService {

    public static void checkout(Customer customer, Cart cart) {
        if (cart.isEmpty()) {
            throw new IllegalStateException("Cart is empty. Cannot proceed to checkout.");
        }

        List<CartItem> items = cart.getItems();
        List<Shippable> shippableItems = new ArrayList<>();

        double subtotal = 0;

        for (CartItem item : items) {
            Product product = item.getProduct();
            int requestedQty = item.getQuantity();

            if (product instanceof Expirable) {
                Expirable exp = (Expirable) product;
                if (exp.isExpired()) {
                    throw new IllegalStateException("Product " + exp.getName() + " is expired.");
                }
            }

            if (requestedQty > product.getQuantity()) {
                throw new IllegalStateException("Not enough stock for product: " + product.getName());
            }

            if (product instanceof Shippable) {
                for (int i = 0; i < requestedQty; i++) {
                    shippableItems.add((Shippable) product);
                }
            }

            subtotal += item.getTotalPrice();
        }

        double shippingFee = ShippingService.calculateShippingFee(shippableItems);
        double total = subtotal + shippingFee;

        if (customer.getBalance() < total) {
            throw new IllegalStateException("Customer balance is insufficient.");
        }

        customer.deductBalance(total);

        for (CartItem item : items) {
            item.getProduct().reduceQuantity(item.getQuantity());
        }

        if (!shippableItems.isEmpty()) {
            System.out.println("** Shipment notice **");
            Map<String, Integer> shipmentCounts = new HashMap<>();
            double totalWeight = 0;

            for (Shippable s : shippableItems) {
                shipmentCounts.put(s.getName(), shipmentCounts.getOrDefault(s.getName(), 0) + 1);
                totalWeight += s.getWeight();
            }

            for (Map.Entry<String, Integer> entry : shipmentCounts.entrySet()) {
                System.out.println(entry.getValue() + "x " + entry.getKey());
            }

            System.out.println("Total package weight " + totalWeight + "kg");
        }

        System.out.println("** Checkout receipt **");
        for (CartItem item : items) {
            System.out.println(item.getQuantity() + "x " + item.getProduct().getName() + " " + item.getTotalPrice());
        }
        System.out.println("----------------------");
        System.out.println("Subtotal " + subtotal);
        System.out.println("Shipping " + shippingFee);
        System.out.println("Amount " + total);
        System.out.println("Customer balance after payment: " + customer.getBalance());
    }
}

public class MyClass {
    public static void main(String[] args) {
        ExpirableShippableProduct cheese = new ExpirableShippableProduct("Cheese 400g", 100, 5, LocalDate.now().plusDays(5), 0.2);
        ExpirableShippableProduct biscuits = new ExpirableShippableProduct("Biscuits 700g", 150, 2, LocalDate.now().plusDays(3), 0.7);
        ShippableProduct tv = new ShippableProduct("TV", 5000, 3, 8);
        Product scratchCard = new Product("Scratch Card", 50, 10);

        Customer customer = new Customer("Mahmoud", 10000);
        Cart cart = new Cart();
        cart.add(cheese, 2);
        cart.add(biscuits, 1);
        cart.add(tv, 1);
        cart.add(scratchCard, 1);

        CheckoutService.checkout(customer, cart);
    }
}
