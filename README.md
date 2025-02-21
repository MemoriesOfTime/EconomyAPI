# EconomyAPI
Core of economy system for Nukkit

## Commands
 - /mymoney
 - /seemoney
 - /givemoney
 - /takemoney
 - /topmoney
 - /setmoney

## Permissions
- economyapi
	- economyapi.command
		- economyapi.command.mymoney
		- economyapi.command.givemoney `OP`
		- economyapi.command.takemoney `OP`
		- economyapi.command.setmoney `OP`
		- economyapi.command.topmoney

## For developers

> [!WARNING]
> MySQL provider:
> Due to precision issues, MySQL uses bigint to store monetary amounts. The retrieved values need to be divided by 100 to convert them to the actual amounts.

###  Single Currency Economies

Developers can access to EconomyAPI's API by using:
```java
EconomyAPI.getInstance().myMoney(player);
EconomyAPI.getInstance().reduceMoney(player, amount);
EconomyAPI.getInstance().addMoney(player, amount);
```

> [!TIP]
> Operating through the provider:
> - `provider.addMoney(player, amount);`

### Multiple Currency Economies

**`currencyName`:** This parameter is a `String` that specifies the currency you want to interact with.  If your economy system supports multiple currencies (e.g., real-world currencies like "USD", "EUR", "GBP" or in-game items like "Gold", "Silver", "Diamonds"), you can use this parameter to target a specific currency.

If your system only has one currency, use the first set of methods (without the `currencyName` parameter).

```java
EconomyAPI.getInstance().myMoney(player, currencyName);
EconomyAPI.getInstance().reduceMoney(player, amount, currencyName);
EconomyAPI.getInstance().addMoney(player, amount, currencyName);
```

> [!TIP]
> When operating through the provider, the parameter order needs to be adjusted:
> - `provider.addMoney(currencyName, player, amount);`

### Maven repository

```xml
<repository>
    <id>nukkitx-repo</id>
    <url>https://repo.nukkitx.com/snapshot</url>
</repository>

<dependency>
    <groupId>me.onebone</groupId>
    <artifactId>economyapi</artifactId>
    <version>2.0.2</version>
    <scope>provided</scope>
</dependency>
```
