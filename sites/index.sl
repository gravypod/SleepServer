<?sleep
	$start = ticks();
?><html>
	

	
	<head>
		<title><?sleep
					print("a");
				?></title>
	</head>
	
	<body>
		<?sleep
			print("Data sent is a:" . %__DATA__["a"] . "</br>");
		?>

	</body>
	<footer>
	
		<?sleep
			
			print("this is a footer</br>");
			
			$time = (ticks() - $start) / 1000.0;
			
			print("Calculation took $time seconds</br>");
			
			print("This will be seen: " . $start);
			
		?>
	
	
	</footer>
	
</html>
