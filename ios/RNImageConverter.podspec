
Pod::Spec.new do |s|
  s.name         = "RNImageConverter"
  s.version      = "1.0.0"
  s.summary      = "RNImageConverter"
  s.description  = <<-DESC
                  RNImageConverter
                   DESC
  s.homepage     = "https://github.com/LeeKyoungIl/react-native-image-converter#readme"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/author/RNImageConverter.git", :tag => "master" }
  s.source_files  = "RNImageConverter/**/*.{h,m}"
  s.requires_arc = true


  s.dependency "React"
  #s.dependency "others"

end

  
